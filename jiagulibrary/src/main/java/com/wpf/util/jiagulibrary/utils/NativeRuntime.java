package com.wpf.util.jiagulibrary.utils;

import android.content.Context;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import dalvik.system.PathClassLoader;

/** Prepend decrypted libraries to the existing loader without running business JNI_OnLoad. */
public final class NativeRuntime {
    public static void install(Context context) {
        String version = IntegrityVerifier.nativeVersion();
        if (version == null) return;
        try {
            NativeArchive.Restored restored = NativeArchive.restore(new File(context.getApplicationInfo().sourceDir),
                new File(context.getNoBackupFilesDir(), "jiagu-native"), version, packageAbi(context), NativeArchive.KEY_HEX);
            if (restored == null) return;
            StartupMetrics.flag("native_cache_hit", restored.cacheHit);
            prepend(context.getClassLoader(), restored.directory);
        } catch (Exception error) { throw new IllegalStateException("Cannot prepare encrypted native libraries", error); }
    }
    private static String packageAbi(Context context) {
        // Business libraries can use the native bridge while the shell follows the actual ART ISA.
        String installed = new File(context.getApplicationInfo().nativeLibraryDir).getName();
        if (installed.equals("arm64")) return "arm64-v8a";
        if (installed.equals("arm")) return "armeabi-v7a";
        if (installed.equals("x86") || installed.equals("x86_64")) return installed;
        return ShellAbi.detect();
    }
    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    private static void prepend(ClassLoader loader, File directory) throws Exception {
        Field path = field(loader.getClass(), "pathList"); Object list = path.get(loader);
        Field dirs = field(list.getClass(), "nativeLibraryDirectories"); Object old = dirs.get(list);
        if (old instanceof File[]) {
            File[] files = (File[]) old; File[] merged = new File[files.length + 1]; merged[0] = directory;
            System.arraycopy(files, 0, merged, 1, files.length); dirs.set(list, merged);
        } else {
            List<File> merged = new ArrayList<>(); merged.add(directory);
            merged.addAll((List<File>) old); dirs.set(list, merged);
            // A temporary empty loader creates the platform-specific element type; it defines no app classes.
            PathClassLoader probe = new PathClassLoader("", directory.getAbsolutePath(), loader.getParent());
            Object probeList = path.get(probe);
            Field elements = field(list.getClass(), "nativeLibraryPathElements");
            Object before = elements.get(list), added = elements.get(probeList);
            int n = Array.getLength(before);
            if (Array.getLength(added) == 0) throw new IllegalStateException("Native directory was not resolved");
            Object combined = Array.newInstance(before.getClass().getComponentType(), n + 1);
            Array.set(combined, 0, Array.get(added, 0)); System.arraycopy(before, 0, combined, 1, n);
            elements.set(list, combined);
        }
    }
}
