#include <jni.h>
extern int compat_dependency(void);
static jint compute(JNIEnv *env, jclass cls) { return compat_dependency() + 1; }
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = 0;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
    jclass cls = (*env)->FindClass(env, "com/wpf/jiagu/compat/NativeChecks");
    if (!cls) return JNI_ERR;
    JNINativeMethod method = {"compute", "()I", (void *)compute};
    if ((*env)->RegisterNatives(env, cls, &method, 1) != JNI_OK) return JNI_ERR;
    return JNI_VERSION_1_6;
}
