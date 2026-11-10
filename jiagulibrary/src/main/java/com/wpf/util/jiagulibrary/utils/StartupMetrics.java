package com.wpf.util.jiagulibrary.utils;

import android.os.Build;
import android.os.Process;
import android.util.Log;
import java.util.LinkedHashMap;
import java.util.Map;

/** Per-process shell timings; does not claim to measure Activity first frame or whole cold start. */
public final class StartupMetrics {
    public static final String TAG = "NDK_JIAGU_PERF";
    private static long start, attached;
    private static boolean failed;
    private static final Map<String, Long> phases = new LinkedHashMap<>();
    public static synchronized void flag(String name, boolean value) { phases.put(name, value ? 1L : 0L); }
    public static synchronized void start() { start = System.nanoTime(); attached = 0; failed = false; phases.clear(); }
    public static synchronized void phase(String name, long since) { phases.put(name, (System.nanoTime() - since) / 1000000L); }
    public static synchronized void attached() { attached = System.nanoTime(); emit("attach", "ok", null); }
    public static synchronized void applicationStarted() {
        if (attached > 0) phases.put("framework_provider_gap_ms", (System.nanoTime() - attached) / 1000000L);
    }
    public static synchronized void complete() { emit("application", failed ? "failed" : "ok", null); }
    public static synchronized void failure(String stage, Throwable error) { failed = true; emit(stage, "failed", error); }
    private static void emit(String stage, String status, Throwable error) {
        StringBuilder json = new StringBuilder("{\"schemaVersion\":1,\"stage\":\"").append(stage)
            .append("\",\"status\":\"").append(status).append("\",\"pid\":").append(Process.myPid())
            .append(",\"sdk\":").append(Build.VERSION.SDK_INT).append(",\"elapsedMs\":")
            .append((System.nanoTime() - start) / 1000000L).append(",\"phasesMs\":{");
        boolean first = true;
        for (Map.Entry<String, Long> entry : phases.entrySet()) {
            if (!first) json.append(','); first = false;
            json.append('"').append(entry.getKey()).append("\":").append(entry.getValue());
        }
        json.append('}');
        if (error != null) json.append(",\"errorType\":\"").append(error.getClass().getName()).append('"');
        json.append('}');
        try { Log.i(TAG, json.toString()); } catch (RuntimeException ignored) { /* telemetry cannot change app behavior */ }
    }
}
