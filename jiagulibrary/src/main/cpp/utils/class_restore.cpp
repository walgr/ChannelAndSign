#include "class_restore.h"
#include "method_restore.h"
#include "And64InlineHook.hpp"
#include "x86_hook.h"
#include "dlopen.h"
#include "art_symbol.h"
#include "plog.h"
#include <memory>
#include <mutex>
#include <sys/mman.h>
#include <unistd.h>

namespace {
std::mutex restoreMutex;
std::vector<std::unique_ptr<method_restore::Dex>> dexFiles;
JavaVM *javaVm = nullptr;
void *artHandle = nullptr; // Keep ART and the trampoline alive for subsequent class loads.
using DefineClass = void *(*)(void *, void *, const char *, size_t, void *, const void *, const void *);
DefineClass originalDefineClass = nullptr;
using DefineClassWithLength = void *(*)(void *, void *, const char *, size_t, size_t, void *, const void *, const void *);
DefineClassWithLength originalDefineClassWithLength = nullptr;

bool restoreClass(const void *dexFile, const void *classDef) {
    std::lock_guard<std::mutex> lock(restoreMutex);
    // Standard ART DexFile prefix: vtable, begin_. No ArtMethod layout dependency.
    const uint8_t *begin = nullptr;
    std::memcpy(&begin, static_cast<const uint8_t *>(dexFile) + sizeof(void *), sizeof(begin));
    uintptr_t base = reinterpret_cast<uintptr_t>(begin);
    uintptr_t def = reinterpret_cast<uintptr_t>(classDef);
    if (def < base) return true;
    const size_t offset = def - base;
    for (const auto &dex : dexFiles) {
        if (offset < dex->classDefsOffset ||
            (offset - dex->classDefsOffset) % 32 != 0 ||
            (offset - dex->classDefsOffset) / 32 >= dex->classDefsCount) continue;
        // ART may copy the ByteBuffer. Match the DEX signature and class definition,
        // then patch ART's begin_, never the now unrelated input buffer.
        if (std::memcmp(begin, dex->source, 112) != 0 ||
            std::memcmp(classDef, dex->source + offset, 32) != 0) continue;
        const auto entry = dex->classes.find(static_cast<uint32_t>(offset));
        if (entry == dex->classes.end()) return true;
        const long pageSize = sysconf(_SC_PAGESIZE);
        if (pageSize <= 0) return false;
        for (uint32_t insns : entry->second) {
            const auto &bytes = dex->instructions.at(insns);
            if (bytes.empty()) continue;
            uintptr_t start = base + insns;
            uintptr_t page = start - start % pageSize;
            size_t length = start - page + bytes.size();
            // DEX instructions are data interpreted/JIT-compiled by ART, not native code.
            if (mprotect(reinterpret_cast<void *>(page), length, PROT_READ | PROT_WRITE) != 0) return false;
            std::memcpy(reinterpret_cast<void *>(start), bytes.data(), bytes.size());
        }
        LOGD("Restored %zu extracted methods at class_def=%zu", entry->second.size(), offset);
        return true;
    }
    return true; // Framework and shell classes have no extracted instructions.
}

void *defineClass(void *linker, void *thread, const char *descriptor, size_t hash,
                  void *loader, const void *dexFile, const void *classDef) {
    if (!restoreClass(dexFile, classDef)) {
        LOGE("Cannot restore extracted methods for %s", descriptor);
        JNIEnv *env = nullptr;
        if (javaVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4) == JNI_OK) {
            jclass error = env->FindClass("java/lang/IllegalStateException");
            if (error) {
                env->ThrowNew(error, "Cannot make extracted DEX instructions writable");
                env->DeleteLocalRef(error);
            }
        }
        return nullptr;
    }
    return originalDefineClass(linker, thread, descriptor, hash, loader, dexFile, classDef);
}
void *defineClassWithLength(void *linker, void *thread, const char *descriptor, size_t first,
                           size_t second, void *loader, const void *dexFile, const void *classDef) {
    if (!restoreClass(dexFile, classDef)) {
        JNIEnv *env = nullptr;
        if (javaVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4) == JNI_OK)
            env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "Cannot restore DEX instructions");
        return nullptr;
    }
    return originalDefineClassWithLength(linker, thread, descriptor, first, second, loader, dexFile, classDef);
}
} // namespace

bool register_method_dex(const uint8_t *dex, size_t size, const uint8_t *data, size_t dataSize) {
    std::unique_ptr<method_restore::Dex> parsed(new method_restore::Dex());
    if (!parsed->parse(dex, size, data, dataSize)) return false;
    std::lock_guard<std::mutex> lock(restoreMutex);
    dexFiles.push_back(std::move(parsed));
    return true;
}

bool install_class_restore(JNIEnv *env, int sdk) {
    bool hasMethods = false;
    for (const auto &dex : dexFiles) hasMethods |= !dex->instructions.empty();
    if (!hasMethods || originalDefineClass || originalDefineClassWithLength) return true;
#if defined(__aarch64__) || defined(__i386__) || defined(__x86_64__)
    if (sdk < 26) return false;
    // Android 10+ ART is delivered in APEX; Android 11+ uses com.android.art.
#if defined(__LP64__)
#define ART_LIB_DIR "lib64"
#define ART_SIZE_T "m"
#else
#define ART_LIB_DIR "lib"
#define ART_SIZE_T "j"
#endif
    const char *paths[] = {"/apex/com.android.art/" ART_LIB_DIR "/libart.so",
                           "/apex/com.android.runtime/" ART_LIB_DIR "/libart.so",
                           "/system/" ART_LIB_DIR "/libart.so"};
    const char *artPath = nullptr;
    for (const char *path : paths) {
        artHandle = ndk_dlopen(path, RTLD_NOW);
        if (artHandle) { artPath = path; break; }
    }
    if (!artHandle) return false;
    const char *symbols[] = {
        "_ZN3art11ClassLinker11DefineClassEPNS_6ThreadEPKc" ART_SIZE_T "NS_6HandleINS_6mirror11ClassLoaderEEERKNS_7DexFileERKNS_3dex8ClassDefE",
        "_ZN3art11ClassLinker11DefineClassEPNS_6ThreadEPKc" ART_SIZE_T "NS_6HandleINS_6mirror11ClassLoaderEEERKNS_7DexFileERKNS9_8ClassDefE"
    };
    void *target = nullptr;
    for (const char *symbol : symbols) {
        target = resolve_art_symbol(artHandle, artPath, symbol);
        if (target) break;
    }
    bool withLength = false;
    if (!target) {
        target = resolve_art_symbol(artHandle, artPath,
            "_ZN3art11ClassLinker11DefineClassEPNS_6ThreadEPKc" ART_SIZE_T ART_SIZE_T "NS_6HandleINS_6mirror11ClassLoaderEEERKNS_7DexFileERKNS_3dex8ClassDefE");
        withLength = target != nullptr;
    }
    if (!target || env->GetJavaVM(&javaVm) != JNI_OK) { LOGE("ART DefineClass resolution failed"); return false; }
    void *replacement = withLength ? reinterpret_cast<void *>(defineClassWithLength) : reinterpret_cast<void *>(defineClass);
    void **original = withLength ? reinterpret_cast<void **>(&originalDefineClassWithLength) : reinterpret_cast<void **>(&originalDefineClass);
#if defined(__aarch64__)
    A64HookFunction(target, replacement, original);
    return *original != nullptr;
#else
    return install_x86_hook(target, replacement, original);
#endif
#else
    return false; // No silent fallback to eager, whole-DEX restoration.
#endif
}
