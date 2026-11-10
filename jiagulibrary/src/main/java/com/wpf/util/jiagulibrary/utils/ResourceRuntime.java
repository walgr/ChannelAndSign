package com.wpf.util.jiagulibrary.utils;

import android.content.Context;
import android.os.Build;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Install decrypted resources before the original Application and all Providers are attached. */
public final class ResourceRuntime {
    public static void install(Context context) {
        String version = IntegrityVerifier.resourceVersion();
        if (version == null) return;
        try {
            File directory = new File(context.getNoBackupFilesDir(), "jiagu-resources");
            ResourceArchive.Restored restored = ResourceArchive.restore(new File(context.getApplicationInfo().sourceDir),
                    directory, version, ResourceArchive.KEY_HEX);
            if (restored == null) throw new IllegalStateException("Verified resource payload is missing");
            StartupMetrics.flag("resource_cache_hit", restored.cacheHit);
            if (Build.VERSION.SDK_INT >= 30) Api30.install(context, restored.file);
            else installLegacy(context, restored.file);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot decrypt or mount resource payload", error);
        }
    }
    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try { Field found = current.getDeclaredField(name); found.setAccessible(true); return found; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    private static void installLegacy(Context context, File archive) throws Exception {
        Object loaded = field(context.getClass(), "mPackageInfo").get(context);
        if (loaded == null) throw new IllegalStateException("No LoadedApk for resource mount");
        // ART 5.x keeps the first table's cookies: appending an archive to the old manager is insufficient.
        field(loaded.getClass(), "mResDir").set(loaded, archive.getAbsolutePath());
        Field splitField = field(loaded.getClass(), "mSplitResDirs");
        String[] splits = (String[]) splitField.get(loaded);
        String[] paths = new String[(splits == null ? 0 : splits.length) + 1];
        paths[0] = context.getApplicationInfo().sourceDir; // fallback for assets excluded by the speed budget
        if (splits != null) System.arraycopy(splits, 0, paths, 1, splits.length);
        splitField.set(loaded, paths);
        field(loaded.getClass(), "mResources").set(loaded, null);
        Object thread = field(context.getClass(), "mMainThread").get(context);
        Object rebuilt = null;
        for (Method method : loaded.getClass().getDeclaredMethods()) {
            if (method.getName().equals("getResources") && method.getParameterTypes().length <= 1) {
                method.setAccessible(true);
                rebuilt = method.getParameterTypes().length == 0 ? method.invoke(loaded) : method.invoke(loaded, thread);
                break;
            }
        }
        if (!(rebuilt instanceof android.content.res.Resources)) throw new IllegalStateException("Cannot rebuild framework resources");
        field(context.getClass(), "mResources").set(context, rebuilt);
    }
    private static final class Api30 {
        private static android.content.res.loader.ResourcesLoader loader;
        static void install(Context context, File archive) throws java.io.IOException {
            loader = new android.content.res.loader.ResourcesLoader();
            try (android.os.ParcelFileDescriptor fd = android.os.ParcelFileDescriptor.open(archive, android.os.ParcelFileDescriptor.MODE_READ_ONLY)) {
                loader.addProvider(android.content.res.loader.ResourcesProvider.loadFromApk(fd));
            }
            // Framework activity and configuration contexts inherit Application resource loaders.
            context.getResources().addLoaders(loader);
        }
    }
}
