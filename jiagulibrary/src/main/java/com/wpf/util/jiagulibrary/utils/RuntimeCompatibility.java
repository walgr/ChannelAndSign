package com.wpf.util.jiagulibrary.utils;

/** Keep the supported runtime contract explicit before loading the native shell. */
public final class RuntimeCompatibility {
    public static void requireSupported(int sdk, String abi) {
        if (sdk < 21 || !("armeabi-v7a".equals(abi) || "arm64-v8a".equals(abi) || "x86".equals(abi) || "x86_64".equals(abi))) {
            throw new IllegalStateException("Method restoration requires Android API 21+ and armeabi-v7a/arm64-v8a/x86/x86_64 ART; API=" + sdk + ", processAbi=" + abi);
        }
    }
}
