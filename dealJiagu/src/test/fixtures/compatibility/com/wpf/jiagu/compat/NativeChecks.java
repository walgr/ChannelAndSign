package com.wpf.jiagu.compat;

public final class NativeChecks {
    static {
        System.loadLibrary("compat_entry");
        System.loadLibrary("compat_standalone");
    }
    private static native int compute();
    private static native int standalone();
    public static void requireValid() {
        if (compute() != 42 || standalone() != 7) throw new AssertionError("Encrypted JNI dependency/registration mismatch");
        android.util.Log.i("JIAGU_COMPAT", "COMPAT_NATIVE_PASSED pid=" + android.os.Process.myPid());
    }
}
