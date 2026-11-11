//
// Created by Ming on 2021/9/7.
//

#include <jni.h>
#include <string>
#include <vector>
#include <new>
#include <thread>
#include <algorithm>
#include<sys/mman.h>
#include<sys/types.h>
#include<fcntl.h>
#include <unistd.h>
#include <errno.h>

#include "utils/plog.h"
#include "utils/invoke_java.h"
#include "utils/aes.h"
#include "utils/dlopen.h"
#include "utils/memload.h"
#include "utils/class_restore.h"
#include "utils/method_restore.h"

#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

#if defined(__LP64__)
#define LIB_ART_PATH "/system/lib64/libart.so"
#elif defined(__arm__)
#define LIB_ART_PATH "/system/lib/libart.so"
#else
#define LIB_ART_PATH "/system/lib/libart.so"
#endif

static jobject g_context;
static int g_sdk_int;
static std::string g_jiagu_path;

// vm dex
unsigned char VMDEX[400] = {
        0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x35, 0x00, 0xAD, 0x2A, 0x11, 0x26, 0x1C, 0x47, 0xDE, 0xC9,
        0x5F, 0x16, 0x03, 0x2F, 0xD2, 0xCC, 0x7A, 0xCB, 0x92, 0x13, 0xB3, 0x2A, 0xD0, 0x7A, 0x9B, 0xF5,
        0x90, 0x01, 0x00, 0x00, 0x70, 0x00, 0x00, 0x00, 0x78, 0x56, 0x34, 0x12, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x14, 0x01, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x70, 0x00, 0x00, 0x00,
        0x03, 0x00, 0x00, 0x00, 0x80, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x8C, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x98, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x00, 0x00, 0xA8, 0x00, 0x00, 0x00, 0xC8, 0x00, 0x00, 0x00, 0xC8, 0x00, 0x00, 0x00,
        0xE0, 0x00, 0x00, 0x00, 0xE8, 0x00, 0x00, 0x00, 0xF2, 0x00, 0x00, 0x00, 0x06, 0x01, 0x00, 0x00,
        0x01, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00,
        0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00,
        0x09, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x70, 0x10, 0x01, 0x00, 0x00, 0x00, 0x0E, 0x00,
        0x06, 0x3C, 0x69, 0x6E, 0x69, 0x74, 0x3E, 0x00, 0x08, 0x4C, 0x63, 0x6F, 0x6D, 0x2F, 0x56, 0x6D,
        0x3B, 0x00, 0x12, 0x4C, 0x6A, 0x61, 0x76, 0x61, 0x2F, 0x6C, 0x61, 0x6E, 0x67, 0x2F, 0x4F, 0x62,
        0x6A, 0x65, 0x63, 0x74, 0x3B, 0x00, 0x01, 0x56, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x81, 0x80,
        0x04, 0xC8, 0x01, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x70, 0x00, 0x00, 0x00,
        0x02, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x80, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x00, 0x00, 0x8C, 0x00, 0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00,
        0x98, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0xA8, 0x00, 0x00, 0x00,
        0x01, 0x20, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0xC8, 0x00, 0x00, 0x00, 0x02, 0x20, 0x00, 0x00,
        0x04, 0x00, 0x00, 0x00, 0xE0, 0x00, 0x00, 0x00, 0x00, 0x20, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
        0x09, 0x01, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x14, 0x01, 0x00, 0x00
};

static bool write_vm_dex(const char *vmdex)
{
    FILE *file = fopen(vmdex, "wb");
    if (!file) return false;
    const bool written = fwrite(VMDEX, sizeof(VMDEX), 1, file) == 1;
    const bool closed = fclose(file) == 0;
    return written && closed;
}

static int byte2Int(char *b, int index) {
    return ((b[index] & 0xff) << 24) + ((b[index + 1] & 0xff) << 16) + ((b[index + 2] & 0xff) << 8) +(b[index + 3] & 0xff);
}

//-------------------------

static void init(JNIEnv *env, jobject application) {
    SetEnv(env);
    ndk_init(env);

    jobject ctx = CallObjectMethod(application, "getBaseContext", "()Landroid/content/Context;").l;
    g_context = env->NewGlobalRef(ctx);

    g_sdk_int = GetStaticField("android/os/Build$VERSION", "SDK_INT", "I").i;

    if (g_sdk_int < 26) {
        jstring dex = env->NewStringUTF(".jiagu");
        jobject fileDir = CallObjectMethod(application, "getFilesDir", "()Ljava/io/File;").l;
        jobject dataDir = CallObjectMethod(fileDir, "getParentFile", "()Ljava/io/File;").l;
        jobject dexDir = NewClassInstance("java/io/File", "(Ljava/io/File;Ljava/lang/String;)V",
                                          dataDir, dex);

        CallObjectMethod(dexDir, "mkdir", "()Z");
        jstring path = static_cast<jstring>(CallObjectMethod(dexDir, "getPath",
                                                             "()Ljava/lang/String;").l);
        // .jiagu目录的路径
        const char *chars = env->GetStringUTFChars(path, nullptr);
        if (!chars) return;
        g_jiagu_path = chars;
        env->ReleaseStringUTFChars(path, chars);

        env->DeleteLocalRef(dex);
        env->DeleteLocalRef(fileDir);
        env->DeleteLocalRef(dataDir);
        env->DeleteLocalRef(dexDir);
        env->DeleteLocalRef(path);

    }
}

static void restore_error(JNIEnv *env, const char *message);

static jobject getDex(JNIEnv *env, jobject application) {
    //获取apk安装路径
    jobject appInfo = CallObjectMethod(application, "getApplicationInfo", "()Landroid/content/pm/ApplicationInfo;").l;
    jstring sourceDir = static_cast<jstring>(GetField(appInfo, "sourceDir",
                                                      "Ljava/lang/String;").l);
    // 获取dex数据
    jobject source = CallObjectMethod(application, "invoke1",
            "(Ljava/lang/String;)Lcom/wpf/util/jiagulibrary/utils/DexSource;", sourceDir).l;
    env->DeleteLocalRef(appInfo);
    env->DeleteLocalRef(sourceDir);
    return source;
}

class PayloadReader {
    JNIEnv *env;
    jobject source;
    jclass type = nullptr;
    jbyteArray chunk = nullptr;
    jmethodID readMethod = nullptr, closeMethod = nullptr;
    size_t chunkOffset = 0, chunkSize = 0, prefixOffset = 0;
    char prefix[528];
    uint32_t shellSize = 0;
    bool raw(void *destination, size_t length) {
        auto *out = static_cast<jbyte *>(destination);
        while (length) {
            if (chunkOffset == chunkSize) {
                jint count = env->CallIntMethod(source, readMethod, chunk);
                if (env->ExceptionCheck()) return false;
                if (count <= 0) { restore_error(env, "Truncated protected DEX stream"); return false; }
                chunkOffset = 0; chunkSize = static_cast<size_t>(count);
            }
            size_t count = std::min(length, chunkSize - chunkOffset);
            env->GetByteArrayRegion(chunk, chunkOffset, count, out);
            if (env->ExceptionCheck()) return false;
            out += count; length -= count; chunkOffset += count;
        }
        return true;
    }
public:
    size_t remaining = 0;
    PayloadReader(JNIEnv *jni, jobject input) : env(jni), source(input) {}
    bool init() {
        type = env->GetObjectClass(source);
        closeMethod = env->GetMethodID(type, "close", "()V");
        if (!closeMethod) return false;
        readMethod = env->GetMethodID(type, "read", "([B)I");
        if (!readMethod) return false;
        jmethodID start = env->GetMethodID(type, "startPayload", "()J");
        if (!start) return false;
        jlong payload = env->CallLongMethod(source, start);
        if (env->ExceptionCheck()) return false;
        jfieldID lengthField = env->GetFieldID(type, "length", "J");
        if (!lengthField) return false;
        jlong total = env->GetLongField(source, lengthField);
        if (payload < 528 || payload > total - 4) { restore_error(env, "Invalid payload size"); return false; }
        shellSize = static_cast<uint32_t>(total - payload - 4);
        chunk = env->NewByteArray(65536);
        if (!chunk || !raw(prefix, sizeof(prefix))) return false;
        int size = 0;
        if (!tiny_aes_decrypt_cbc(prefix, sizeof(prefix), &size) || size != 512) {
            restore_error(env, "Cannot decrypt DEX payload"); return false;
        }
        remaining = static_cast<size_t>(payload) - 16;
        return true;
    }
    bool read(void *destination, size_t length) {
        if (length == 0) return true;
        if (length > remaining) { restore_error(env, "Invalid protected DEX record length"); return false; }
        auto *out = static_cast<uint8_t *>(destination);
        size_t first = std::min(length, size_t(512) - prefixOffset);
        memcpy(out, prefix + prefixOffset, first);
        prefixOffset += first;
        if (!raw(out + first, length - first)) return false;
        remaining -= length;
        return true;
    }
    bool word(uint32_t &value) {
        uint8_t bytes[4];
        if (!read(bytes, sizeof(bytes))) return false;
        value = method_restore::be32(bytes); return true;
    }
    bool finish() {
        uint8_t footer[4];
        if (remaining || !raw(footer, 4) || method_restore::be32(footer) != shellSize || chunkOffset != chunkSize) {
            restore_error(env, "Invalid protected DEX footer"); return false;
        }
        jint count = env->CallIntMethod(source, readMethod, chunk);
        if (env->ExceptionCheck()) return false;
        if (count != -1) { restore_error(env, "Trailing protected DEX bytes"); return false; }
        return true;
    }
    ~PayloadReader() {
        jthrowable error = env->ExceptionOccurred();
        if (error) env->ExceptionClear();
        if (closeMethod) env->CallVoidMethod(source, closeMethod);
        if (error) { env->ExceptionClear(); env->Throw(error); env->DeleteLocalRef(error); }
        if (chunk) env->DeleteLocalRef(chunk);
        if (type) env->DeleteLocalRef(type);
        env->DeleteLocalRef(source);
    }
};

static jobject openmemory_load_dex(JNIEnv *env, void *art_handle, char *base, int dex_size) {
    // The encrypted payload backing this buffer lives until process exit. Do not write
    // business DEX to disk or restore its instructions before the DefineClass hook.
    const void *nativeDex = load_legacy_dex(g_sdk_int, art_handle, base, dex_size);
    if (!nativeDex) return nullptr;
    const std::string stem = std::string(g_jiagu_path) + "/vm-" + std::to_string(getpid());
    const std::string dex_path = stem + ".dex", odex_path = stem + ".odex";
    if (!write_vm_dex(dex_path.c_str())) return nullptr;
    jstring dex = env->NewStringUTF(dex_path.c_str());
    jstring odex = env->NewStringUTF(odex_path.c_str());
    jobject dexFile = CallStaticMethod("dalvik/system/DexFile", "loadDex",
        "(Ljava/lang/String;Ljava/lang/String;I)Ldalvik/system/DexFile;", dex, odex, 0).l;
    env->DeleteLocalRef(dex); env->DeleteLocalRef(odex);
    // These files contain the fixed 400-byte empty shell DEX, never business code.
    // ART keeps its mappings after unlink; recreate the stub for the next DEX slice.
    unlink(dex_path.c_str()); unlink(odex_path.c_str());
    if (!dexFile || env->ExceptionCheck()) return nullptr;
    jclass type = env->FindClass("dalvik/system/DexFile");
    if (g_sdk_int <= 22) {
        auto *files = new std::vector<const void *>{nativeDex};
        jfieldID field = env->GetFieldID(type, "mCookie", "J");
        if (!field) { delete files; return nullptr; }
        env->SetLongField(dexFile, field, static_cast<jlong>(reinterpret_cast<uintptr_t>(files)));
    } else {
        // Marshmallow: [DexFile*]; Nougat: [OatFile* (null), DexFile*].
        const jsize offset = g_sdk_int == 23 ? 0 : 1;
        jlongArray cookie = env->NewLongArray(offset + 1);
        if (!cookie) return nullptr;
        const jlong pointer = static_cast<jlong>(reinterpret_cast<uintptr_t>(nativeDex));
        env->SetLongArrayRegion(cookie, offset, 1, &pointer);
        jfieldID field = env->GetFieldID(type, "mCookie", "Ljava/lang/Object;");
        if (!field || env->ExceptionCheck()) return nullptr;
        env->SetObjectField(dexFile, field, cookie);
        if (g_sdk_int >= 24) {
            jfieldID internal = env->GetFieldID(type, "mInternalCookie", "Ljava/lang/Object;");
            if (!internal) return nullptr;
            env->SetObjectField(dexFile, internal, cookie);
        }
        env->DeleteLocalRef(cookie);
    }
    env->DeleteLocalRef(type);
    return env->ExceptionCheck() ? nullptr : dexFile;
}
static bool make_dex_elements(JNIEnv *env, jobject classLoader, std::vector<jobject> dexFileobjs)
{
    if (!classLoader || dexFileobjs.empty() || env->ExceptionCheck()) return false;
    jclass BaseDexClassLoader = env->FindClass("dalvik/system/BaseDexClassLoader");
    if (!BaseDexClassLoader || !env->IsInstanceOf(classLoader, BaseDexClassLoader)) return false;
    // get pathList fieldid
    jfieldID pathListid = env->GetFieldID(BaseDexClassLoader, "pathList", "Ldalvik/system/DexPathList;");
    if (!pathListid) return false;
    jobject pathList = env->GetObjectField(classLoader, pathListid);
    if (!pathList || env->ExceptionCheck()) return false;

    // get DexPathList Class
    jclass DexPathListClass = env->GetObjectClass(pathList);
    // get dexElements fieldid
    jfieldID dexElementsid = env->GetFieldID(DexPathListClass, "dexElements", "[Ldalvik/system/DexPathList$Element;");
    if (!dexElementsid) return false;
    jobjectArray dexElement = static_cast<jobjectArray>(env->GetObjectField(pathList, dexElementsid));
    if (!dexElement || env->ExceptionCheck()) return false;

    jint len = env->GetArrayLength(dexElement);

    if (g_sdk_int >= 26) {
        std::vector<jobject> missing;
        for (jobject candidate : dexFileobjs) {
            bool present = false;
            for (int i = 0; i < len; ++i) {
                jobject element = env->GetObjectArrayElement(dexElement, i);
                present |= env->IsSameObject(candidate, element);
                env->DeleteLocalRef(element);
            }
            if (!present) missing.push_back(candidate);
        }
        dexFileobjs.swap(missing);
    }
    LOGD("[+]Elements size:%d, append memory elements:%zu", len, dexFileobjs.size());

    // Get dexElement all values and add  add each value to the new array
    jclass ElementClass = env->FindClass(
            "dalvik/system/DexPathList$Element"); // dalvik/system/DexPathList$Element
    jobjectArray new_dexElement = env->NewObjectArray(len + dexFileobjs.size(), ElementClass, NULL);
    if (!new_dexElement) return false;
    for (int i = 0; i < len; i++)
    {
        env->SetObjectArrayElement(new_dexElement, i, env->GetObjectArrayElement(dexElement, i));
    }

    if (g_sdk_int < 26) {
        jmethodID ElementInit = env->GetMethodID(ElementClass, "<init>",
                                                 "(Ljava/io/File;ZLjava/io/File;Ldalvik/system/DexFile;)V");
        if (!ElementInit) return false;
        jboolean isDirectory = JNI_FALSE;
        for (int i = 0; i < dexFileobjs.size(); i++) {
            jobject element_obj = env->NewObject(ElementClass, ElementInit, NULL, isDirectory, NULL,
                                                 dexFileobjs[i]);
            env->SetObjectArrayElement(new_dexElement, len + i, element_obj);
        }
    } else {
        for (int i = 0; i < dexFileobjs.size(); i++) {
            env->SetObjectArrayElement(new_dexElement, len + i, dexFileobjs[i]);
        }
    }
    env->SetObjectField(pathList, dexElementsid, new_dexElement);
    bool success = !env->ExceptionCheck();

    env->DeleteLocalRef(ElementClass);
    env->DeleteLocalRef(dexElement);
    env->DeleteLocalRef(DexPathListClass);
    env->DeleteLocalRef(pathList);
    env->DeleteLocalRef(BaseDexClassLoader);
    env->DeleteLocalRef(new_dexElement);
    return success;
}

static void hook_application(jobject app, jstring name) {
    CallObjectMethod(app,  "invoke2", "(Landroid/app/Application;Ljava/lang/String;)V", app, name);
}


static void restore_error(JNIEnv *env, const char *message) {
    LOGE("%s", message);
    if (env->ExceptionCheck()) return;
    jclass error = env->FindClass("java/lang/IllegalStateException");
    if (error) {
        env->ThrowNew(error, message);
        env->DeleteLocalRef(error);
    }
}

// DirectByteBuffer and the DefineClass hook outlive attachBaseContext.

/**
 * Dex加密格式
 * 壳dex + (1字节application名长度 + app的application名 + 4字节源dex大小 + 源dex + 4字节源dex抽取代码大小 + 源dex抽取代码) + 4字节源dex2大小 + [源dex2] + 4字节源dex2抽取代码大小 + 源dex2抽取代码+ 4字节源dex3大小 + [源dex3] + ... + 4字节壳dex大小
 * 小括号的数据前512字节进行AES加密
 * 中括号的数据的dex头(也就是前112位进行异或)
 */
// ART associates a DexFile with its defining loader. Creating a temporary
// InMemoryDexClassLoader and transplanting its elements violates that ownership.
static jobjectArray memory_elements(JNIEnv *env, jobject owner, const std::vector<jobject> &buffers) {
    jclass bufferType = env->FindClass("java/nio/ByteBuffer");
    jobjectArray array = env->NewObjectArray(buffers.size(), bufferType, nullptr);
    if (!array) return nullptr;
    for (size_t i = 0; i < buffers.size(); ++i) env->SetObjectArrayElement(array, i, buffers[i]);
    jclass type = env->FindClass("dalvik/system/DexPathList");
    jmethodID constructor = nullptr, init = nullptr;
    jthrowable lookupError = nullptr;
    JavaVM *vm = nullptr;
    env->GetJavaVM(&vm);
    // Resolve platform internals on an attached native thread, without an app
    // Java frame being mistaken for the defining platform caller. No VM-wide
    // hidden-API exemptions or device settings are changed.
    std::thread lookup([&] {
        JNIEnv *worker = nullptr;
        if (vm->AttachCurrentThread(&worker, nullptr) != JNI_OK) return;
        jclass paths = worker->FindClass("dalvik/system/DexPathList");
        if (paths) constructor = worker->GetMethodID(paths, "<init>", "(Ljava/lang/ClassLoader;Ljava/lang/String;)V");
        if (constructor && !worker->ExceptionCheck()) init = worker->GetMethodID(paths, "initByteBufferDexPath", "([Ljava/nio/ByteBuffer;)V");
        if (worker->ExceptionCheck()) {
            jthrowable error = worker->ExceptionOccurred();
            worker->ExceptionClear();
            lookupError = static_cast<jthrowable>(worker->NewGlobalRef(error));
        }
        vm->DetachCurrentThread();
    });
    lookup.join();
    if (lookupError) {
        env->Throw(lookupError);
        env->DeleteGlobalRef(lookupError);
        return nullptr;
    }
    if (!constructor || !init) return nullptr;
    jobject paths = env->NewObject(type, constructor, owner, nullptr);
    if (!paths || env->ExceptionCheck()) return nullptr;
    env->CallVoidMethod(paths, init, array);
    if (env->ExceptionCheck()) return nullptr;
    jobjectArray elements = static_cast<jobjectArray>(GetField(paths, "dexElements", "[Ldalvik/system/DexPathList$Element;").l);
    if (elements && env->GetArrayLength(elements) == 0) {
        jobjectArray failures = static_cast<jobjectArray>(GetField(paths, "dexElementsSuppressedExceptions", "[Ljava/io/IOException;").l);
        if (failures && env->GetArrayLength(failures)) env->Throw(static_cast<jthrowable>(env->GetObjectArrayElement(failures, 0)));
        else restore_error(env, "No in-memory DEX elements for application loader");
        elements = nullptr;
    }
    env->DeleteLocalRef(paths);
    env->DeleteLocalRef(type);
    env->DeleteLocalRef(array);
    env->DeleteLocalRef(bufferType);
    return elements;
}

// Return ART's copied backing only when the cookie proves input is not retained.
static jobject copied_dex_buffer(JNIEnv *env, jobjectArray elements, const void *input, uint32_t length) {
    const uint8_t *copy = nullptr;
    bool inputUsed = false;
    jclass longs = env->FindClass("[J");
    for (jsize i = 0; i < env->GetArrayLength(elements); ++i) {
        jobject element = env->GetObjectArrayElement(elements, i);
        jobject file = GetField(element, "dexFile", "Ldalvik/system/DexFile;").l;
        jobject cookie = file ? GetField(file, "mCookie", "Ljava/lang/Object;").l : nullptr;
        if (cookie && env->IsInstanceOf(cookie, longs)) {
            auto values = static_cast<jlongArray>(cookie);
            for (jsize n = 1; n < env->GetArrayLength(values); ++n) {
                jlong pointer = 0;
                env->GetLongArrayRegion(values, n, 1, &pointer);
                if (!pointer) continue;
                const uint8_t *begin = nullptr;
                std::memcpy(&begin, reinterpret_cast<const uint8_t *>(static_cast<uintptr_t>(pointer)) + sizeof(void *), sizeof(begin));
                inputUsed |= begin == input;
                if (begin && std::memcmp(begin, input, 112) == 0) copy = begin;
            }
        }
        if (cookie) env->DeleteLocalRef(cookie);
        if (file) env->DeleteLocalRef(file);
        env->DeleteLocalRef(element);
        if (env->ExceptionCheck()) break;
    }
    env->DeleteLocalRef(longs);
    if (env->ExceptionCheck() || inputUsed || !copy) return nullptr;
    return env->NewDirectByteBuffer(const_cast<uint8_t *>(copy), length);
}

static void loadDex(JNIEnv *env, jobject application, jobject source) {

    if (!source || env->ExceptionCheck()) return;
    PayloadReader reader(env, source);
    if (!reader.init()) return;
    uint8_t appLength;
    if (!reader.read(&appLength, 1) || appLength == 0) { restore_error(env, "Invalid application name length"); return; }
    std::string app_name(appLength, '\0');
    if (!reader.read(&app_name[0], appLength)) return;

    struct DexSlice { char *data; uint32_t length; };
    std::vector<DexSlice> slices;
    while (reader.remaining) {
        uint32_t length;
        if (!reader.word(length)) return;
        if (length < 112 || length > reader.remaining) {
            restore_error(env, "Invalid DEX length");
            return;
        }
        // Payload records have no alignment padding. ART 5.x requires 4-byte DEX
        // alignment; later DEX slices may start at an arbitrary byte offset.
        // Keep page-aligned writable backing for both legacy and modern loaders.
        char *begin = static_cast<char *>(mmap(nullptr, length, PROT_READ | PROT_WRITE,
                                             MAP_PRIVATE | MAP_ANONYMOUS, -1, 0));
        if (begin == MAP_FAILED) {
            restore_error(env, "Cannot allocate aligned DEX memory");
            return;
        }
        if (!reader.read(begin, length)) { munmap(begin, length); return; }
        if (!slices.empty()) {
            for (int i = 0; i < 112; ++i) begin[i] ^= 0x66;
        }
        // The producer fixes checksums after extraction. The signed APK covers
        // these bytes; recalculating every DEX checksum here delays large-app startup.
        uint32_t methodsLength;
        if (!reader.word(methodsLength)) return;
        if (methodsLength > reader.remaining) { restore_error(env, "Invalid extracted method length"); return; }
        std::vector<uint8_t> methods(methodsLength);
        if (!reader.read(methods.data(), methods.size())) return;
        if (!register_method_dex(reinterpret_cast<uint8_t *>(begin), length, methods.data(), methods.size())) {
            restore_error(env, "Invalid extracted method records");
            return;
        }
        slices.push_back({begin, length});
    }
    if (!reader.finish()) return;
      // All DEX records must be registered before ART can define any business class.
    if (slices.empty() || !install_class_restore(env, g_sdk_int)) {
        restore_error(env, "Class-based method restoration requires ARM/Intel ART API 21+ and a supported DefineClass hook");
        return;
    }

    void *art_handle = nullptr;
    if (g_sdk_int < 26) {
        art_handle = ndk_dlopen(LIB_ART_PATH, RTLD_NOW);
        if (!art_handle) {
            LOGE("[-]get %s handle failed:%s", LIB_ART_PATH, dlerror());
            restore_error(env, "Cannot open ART for legacy DEX loading");
            return;
        }
    }

    // 加载dex
    jobject classLoader = CallObjectMethod(g_context, "getClassLoader", "()Ljava/lang/ClassLoader;").l;

    std::vector<jobject> dexobjs;
    std::vector<jobject> dexBuffers;

    for (const auto &slice : slices) {
        if (g_sdk_int < 26) {
            jobject mCookie = openmemory_load_dex(env, art_handle, slice.data, slice.length);
            if (mCookie) {
                dexobjs.push_back(mCookie);
            } else {
                restore_error(env, "Cannot load legacy in-memory DEX");
                return;
            }

        } else {
            jobject dexBuffer = env->NewDirectByteBuffer(slice.data, slice.length);
            dexBuffers.push_back(dexBuffer);
        }

    }

    jstring appname = env->NewStringUTF(app_name.c_str());
    if (g_sdk_int < 26) {
        ndk_dlclose(art_handle);
    } else if (g_sdk_int >= 29) {
        size_t released = 0;
        for (size_t n = 0; n < slices.size(); ++n) {
            jobjectArray elements = memory_elements(env, classLoader, {dexBuffers[n]});
            if (!elements || env->ExceptionCheck()) return;
            jobject buffer = copied_dex_buffer(env, elements, slices[n].data, slices[n].length);
            if (env->ExceptionCheck()) return;
            for (jsize i = 0; i < env->GetArrayLength(elements); ++i)
                dexobjs.push_back(env->GetObjectArrayElement(elements, i));
            env->DeleteLocalRef(elements);
            if (buffer) {
                env->DeleteLocalRef(dexBuffers[n]);
                dexBuffers[n] = buffer;
                if (munmap(slices[n].data, slices[n].length) == 0) released += slices[n].length;
            }
        }
        LOGD("Released copied DEX input bytes:%zu", released);
    } else {
        jclass ElementClass = env->FindClass("java/nio/ByteBuffer");
        jobjectArray dexBufferArr = env->NewObjectArray(dexBuffers.size(), ElementClass, NULL);
        for (int i = 0; i < dexBuffers.size(); i++) {
            env->SetObjectArrayElement(dexBufferArr, i, dexBuffers[i]);
        }

        jclass InMemoryDexClassLoaderClass = env->FindClass("dalvik/system/InMemoryDexClassLoader");
        // API 26 only has the single-buffer constructor; the array overload is API 27+.
        jmethodID InMemoryDexClassLoaderInit = env->GetMethodID(InMemoryDexClassLoaderClass, "<init>",
                g_sdk_int == 26 ? "(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V"
                                : "([Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V");
        if (!InMemoryDexClassLoaderInit) return;
        const size_t loaderCount = g_sdk_int == 26 ? dexBuffers.size() : 1;
        for (size_t n = 0; n < loaderCount; ++n) {
            jobject buffers = g_sdk_int == 26 ? dexBuffers[n] : dexBufferArr;
            jobject InMemoryObj = env->NewObject(InMemoryDexClassLoaderClass,
                    InMemoryDexClassLoaderInit, buffers, classLoader);
            if (!InMemoryObj || env->ExceptionCheck()) return;
            jobject pathListObj = GetField(InMemoryObj, "pathList", "Ldalvik/system/DexPathList;").l;
            if (!pathListObj || env->ExceptionCheck()) return;
            jobjectArray dexElements = static_cast<jobjectArray>(
                    GetField(pathListObj, "dexElements", "[Ldalvik/system/DexPathList$Element;").l);
            if (!dexElements || env->ExceptionCheck()) return;
              if (env->GetArrayLength(dexElements) == 0) {
                  jobjectArray failures = static_cast<jobjectArray>(GetField(pathListObj,
                      "dexElementsSuppressedExceptions", "[Ljava/io/IOException;").l);
                  if (failures && env->GetArrayLength(failures)) {
                      env->Throw(static_cast<jthrowable>(env->GetObjectArrayElement(failures, 0)));
                  } else restore_error(env, "InMemoryDexClassLoader produced no DEX elements");
                  return;
              }
            for (int i = 0; i < env->GetArrayLength(dexElements); ++i) {
                dexobjs.push_back(env->GetObjectArrayElement(dexElements, i));
            }
            env->DeleteLocalRef(dexElements);
            env->DeleteLocalRef(pathListObj);
            env->DeleteLocalRef(InMemoryObj);
        }

        env->DeleteLocalRef(ElementClass);
        env->DeleteLocalRef(InMemoryDexClassLoaderClass);
        env->DeleteLocalRef(dexBufferArr);
    }

    if (!make_dex_elements(env, classLoader, dexobjs)) {
        restore_error(env, "Cannot inject business DEX into application ClassLoader");
        return;
    }
    hook_application(application, appname);
    if (env->ExceptionCheck()) return;
    // Original attachBaseContext may replace/reset the loader or its dexElements.
    if (!make_dex_elements(env, classLoader, dexobjs)) {
        restore_error(env, "Original Application removed business DEX elements");
        return;
    }
    jobject currentLoader = CallObjectMethod(g_context, "getClassLoader", "()Ljava/lang/ClassLoader;").l;
    if (!currentLoader || env->ExceptionCheck()) return;
    if (!env->IsSameObject(currentLoader, classLoader)) {
        std::vector<jobject> replacement;
        if (g_sdk_int >= 29) {
              for (jobject live : dexBuffers) {
                  const size_t length = static_cast<size_t>(env->GetDirectBufferCapacity(live));
                  auto *begin = static_cast<uint8_t *>(env->GetDirectBufferAddress(live));
                  auto *snapshot = static_cast<uint8_t *>(mmap(nullptr, length, PROT_READ | PROT_WRITE,
                          MAP_PRIVATE | MAP_ANONYMOUS, -1, 0));
                  if (snapshot == MAP_FAILED || !copy_protected_dex(begin, length, snapshot)) {
                      if (snapshot != MAP_FAILED) munmap(snapshot, length);
                      restore_error(env, "Cannot recreate protected DEX for replacement ClassLoader");
                      return;
                  }
                  jobject buffer = env->NewDirectByteBuffer(snapshot, length);
                  jobjectArray elements = memory_elements(env, currentLoader, {buffer});
                  env->DeleteLocalRef(buffer);
                  if (!elements || env->ExceptionCheck()) { munmap(snapshot, length); return; }
                  jobject copied = copied_dex_buffer(env, elements, reinterpret_cast<char *>(snapshot), length);
                  if (copied) { munmap(snapshot, length); env->DeleteLocalRef(copied); }
                  for (int i = 0; i < env->GetArrayLength(elements); ++i)
                      replacement.push_back(env->GetObjectArrayElement(elements, i));
                  env->DeleteLocalRef(elements);
              }
        }
        const bool injected = make_dex_elements(env, currentLoader, g_sdk_int >= 29 ? replacement : dexobjs);
        for (jobject element : replacement) env->DeleteLocalRef(element);
        if (!injected) {
            restore_error(env, "Cannot inject business DEX into replaced ClassLoader");
            return;
        }
    }
    env->DeleteLocalRef(currentLoader);

    for (int i = 0; i < dexobjs.size(); i++) {
        env->DeleteLocalRef(dexobjs[i]);
    }
    for (int i = 0; i < dexBuffers.size(); i++) {
        env->DeleteLocalRef(dexBuffers[i]);
    }
    env->DeleteLocalRef(classLoader);
    env->DeleteLocalRef(appname);
}

void uninit(JNIEnv *env) {
    env->DeleteGlobalRef(g_context);
}

static jfieldID serviceDispatcherContext = nullptr;
static jfieldID receiverDispatcherContext = nullptr;

extern "C" JNIEXPORT jobject JNICALL
Java_com_wpf_util_jiagulibrary_utils_ApplicationHook_componentFactory(
        JNIEnv *env, jclass, jobject loadedApk, jobject replacement) {
    JavaVM *vm = nullptr;
    env->GetJavaVM(&vm);
    jfieldID field = nullptr;
    jthrowable failure = nullptr;
    // Same bounded framework-field lookup as early callback migration. Do not
    // alter the application's hidden-API policy to restore its host factory.
    std::thread resolve([&] {
        JNIEnv *worker = nullptr;
        if (vm->AttachCurrentThread(&worker, nullptr) != JNI_OK) return;
        jclass loaded = worker->FindClass("android/app/LoadedApk");
        if (loaded) field = worker->GetFieldID(loaded, "mAppComponentFactory",
                "Landroid/app/AppComponentFactory;");
        if (worker->ExceptionCheck()) {
            jthrowable error = worker->ExceptionOccurred();
            worker->ExceptionClear();
            failure = static_cast<jthrowable>(worker->NewGlobalRef(error));
        }
        vm->DetachCurrentThread();
    });
    resolve.join();
    if (failure) { env->Throw(failure); env->DeleteGlobalRef(failure); return nullptr; }
    if (!field) { restore_error(env, "Cannot resolve host component factory"); return nullptr; }
    jobject previous = env->GetObjectField(loadedApk, field);
    if (replacement && !env->ExceptionCheck()) env->SetObjectField(loadedApk, field, replacement);
    return previous;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_wpf_util_jiagulibrary_utils_ApplicationHook_callbackMaps(JNIEnv *env, jclass, jobject loadedApk) {
    JavaVM *vm = nullptr;
    env->GetJavaVM(&vm);
    jfieldID fields[4] = {};
    jthrowable failure = nullptr;
    // Resolve framework-only field IDs without an application frame. This does
    // not change the VM's hidden-API policy for the app or its other libraries.
    std::thread resolve([&] {
        JNIEnv *worker = nullptr;
        if (vm->AttachCurrentThread(&worker, nullptr) != JNI_OK) return;
        jclass loaded = worker->FindClass("android/app/LoadedApk");
        const char *names[] = {"mServices", "mUnboundServices", "mReceivers", "mUnregisteredReceivers"};
        for (int i = 0; loaded && i < 4 && !worker->ExceptionCheck(); i++)
            fields[i] = worker->GetFieldID(loaded, names[i], "Landroid/util/ArrayMap;");
        if (!worker->ExceptionCheck()) {
            jclass service = worker->FindClass("android/app/LoadedApk$ServiceDispatcher");
            if (service) serviceDispatcherContext = worker->GetFieldID(service, "mContext", "Landroid/content/Context;");
        }
        if (!worker->ExceptionCheck()) {
            jclass receiver = worker->FindClass("android/app/LoadedApk$ReceiverDispatcher");
            if (receiver) receiverDispatcherContext = worker->GetFieldID(receiver, "mContext", "Landroid/content/Context;");
        }
        if (worker->ExceptionCheck()) {
            jthrowable error = worker->ExceptionOccurred();
            worker->ExceptionClear();
            failure = static_cast<jthrowable>(worker->NewGlobalRef(error));
        }
        vm->DetachCurrentThread();
    });
    resolve.join();
    if (failure) { env->Throw(failure); env->DeleteGlobalRef(failure); return nullptr; }
    if (!fields[3] || !serviceDispatcherContext || !receiverDispatcherContext) {
        restore_error(env, "Cannot resolve early callback dispatchers"); return nullptr;
    }
    jclass object = env->FindClass("java/lang/Object");
    jobjectArray maps = env->NewObjectArray(4, object, nullptr);
    for (int i = 0; maps && i < 4 && !env->ExceptionCheck(); i++) {
        jobject map = env->GetObjectField(loadedApk, fields[i]);
        env->SetObjectArrayElement(maps, i, map);
        env->DeleteLocalRef(map);
    }
    env->DeleteLocalRef(object);
    return maps;
}

extern "C" JNIEXPORT void JNICALL
Java_com_wpf_util_jiagulibrary_utils_ApplicationHook_setDispatcherContext(
        JNIEnv *env, jclass, jobject dispatcher, jobject context, jboolean service) {
    env->SetObjectField(dispatcher, service ? serviceDispatcherContext : receiverDispatcherContext, context);
}


void native_attach(JNIEnv *env, jclass clazz, jobject application) {
    init(env, application);

    // 从/data/app/xxx/base.apk获取dex
    LOGD("[-]getDex");
    jobject dexData = getDex(env, application);

    // 内存加载dex
    LOGD("[-]loadDex");
    loadDex(env, application, dexData);

    uninit(env);
}


// Dalvik VM type signatures
static const JNINativeMethod gMethods[] = {
        {"attach",          "(Lcom/wpf/util/jiagulibrary/StubApp;)V",      (void *)native_attach},
};


// ----------------------------------------------------------------------------
const char* const kClassPathName = "com/wpf/util/jiagulibrary/StubApp";
int register_natives(JNIEnv *env)
{
    jclass javaClass = env->FindClass(kClassPathName);
    if (javaClass == NULL){
        return JNI_ERR;
    }
    return env->RegisterNatives(javaClass, gMethods, NELEM(gMethods));
}

jint JNI_OnLoad(JavaVM* vm, void* reserved __unused)
{

    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed\n");
        goto bail;
    }

    if (register_natives(env) < 0) {
        LOGE("ERROR: jiagu native registration failed\n");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

    bail:
    return result;
}
