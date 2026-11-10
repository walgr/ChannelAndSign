#include "memload.h"
#include "art_symbol.h"
#include "dex_header.h"
#include "plog.h"
#include <memory>
#include <string>

// AOSP 5.0/5.1 return a raw DexFile; 6.0/7.x return unique_ptr<const DexFile>.
// The pointer is retained by the Java cookie for the process lifetime.
using Open21 = const void *(*)(const uint8_t *, size_t, const std::string &, uint32_t, void *, std::string *);
using Open22 = const void *(*)(const uint8_t *, size_t, const std::string &, uint32_t, void *, const void *, std::string *);
using Open23 = std::unique_ptr<const uint8_t> (*)(const uint8_t *, size_t, const std::string &, uint32_t, void *, const void *, std::string *);

const void *load_legacy_dex(int sdk, void *art, const char *base, size_t size) {
#if defined(__LP64__)
#define DEX_SIZE_T "m"
    const char *path = "/system/lib64/libart.so";
#else
#define DEX_SIZE_T "j"
    const char *path = "/system/lib/libart.so";
#endif
    const char *symbol = sdk == 21
        ? "_ZN3art7DexFile10OpenMemoryEPKh" DEX_SIZE_T "RKNSt3__112basic_stringIcNS3_11char_traitsIcEENS3_9allocatorIcEEEEjPNS_6MemMapEPS9_"
        : sdk == 22
        ? "_ZN3art7DexFile10OpenMemoryEPKh" DEX_SIZE_T "RKNSt3__112basic_stringIcNS3_11char_traitsIcEENS3_9allocatorIcEEEEjPNS_6MemMapEPKNS_7OatFileEPS9_"
        : "_ZN3art7DexFile10OpenMemoryEPKh" DEX_SIZE_T "RKNSt3__112basic_stringIcNS3_11char_traitsIcEENS3_9allocatorIcEEEEjPNS_6MemMapEPKNS_10OatDexFileEPS9_";
    if (sdk < 21 || sdk > 25) return nullptr;
    void *entry = resolve_art_symbol(art, path, symbol);
    if (!entry) { LOGE("ART OpenMemory symbol unavailable for API %d", sdk); return nullptr; }
    std::string location = "Anonymous-Jiagu-" + std::to_string(reinterpret_cast<uintptr_t>(base));
    std::string error;
    const auto *bytes = reinterpret_cast<const uint8_t *>(base);
    const uint32_t checksum = reinterpret_cast<const Header *>(base)->checksum_;
    const void *dex = nullptr;
    if (sdk == 21) dex = reinterpret_cast<Open21>(entry)(bytes, size, location, checksum, nullptr, &error);
    else if (sdk == 22) dex = reinterpret_cast<Open22>(entry)(bytes, size, location, checksum, nullptr, nullptr, &error);
    else dex = reinterpret_cast<Open23>(entry)(bytes, size, location, checksum, nullptr, nullptr, &error).release();
    if (!dex) LOGE("ART OpenMemory rejected DEX on API %d: %s", sdk, error.c_str());
    return dex;
}
