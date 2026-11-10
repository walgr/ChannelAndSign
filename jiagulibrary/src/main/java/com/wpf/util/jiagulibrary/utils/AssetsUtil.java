package com.wpf.util.jiagulibrary.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import com.wpf.util.jiagulibrary.StubApp;
import java.io.*;
import java.util.Objects;

public class AssetsUtil {
    public static synchronized String copyJiaGu(Context context) {
        File dataDir = Objects.requireNonNull(context.getFilesDir().getParentFile());
        File directory = new File(dataDir, ".jiagu");
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IllegalStateException("Cannot create shell directory: " + directory);
        }
        String abi = ShellAbi.detect();
        RuntimeCompatibility.requireSupported(android.os.Build.VERSION.SDK_INT, abi);
        String asset = ShellAbi.assetName(abi);
        long updateTime;
        try {
            updateTime = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).lastUpdateTime;
        } catch (PackageManager.NameNotFoundException error) {
            throw new IllegalStateException("Cannot identify installed shell APK", error);
        }
        File target = new File(directory, "libjiagu" + StubApp.VERSION + "_" + updateTime + "_" + abi + ".so");
        Log.w("NDK_JIAGU", "processAbi=" + abi + ", asset=" + asset + ", target=" + target);
        if (IntegrityVerifier.matchesShell(target, asset)) {
            StartupMetrics.flag("so_cache_hit", true);
            return target.getAbsolutePath();
        }
        StartupMetrics.flag("so_cache_hit", false);

        File temporary = null;
        try {
            // Publish only complete SOs, including when multiple app processes start.
            temporary = File.createTempFile("libjiagu-", ".tmp", directory);
            try (InputStream input = context.getAssets().open(asset);
                 FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                output.getFD().sync();
            }
            if (temporary.length() == 0) throw new IOException("Empty shell SO asset");
            if (!IntegrityVerifier.matchesShell(temporary, asset)) throw new IOException("Extracted shell SO integrity mismatch");
            try (InputStream input = new FileInputStream(temporary)) {
                byte[] header = new byte[20];
                new DataInputStream(input).readFully(header);
                if (!abi.equals(ShellAbi.fromElfHeader(header))) {
                    throw new IOException("Shell SO ELF does not match process ABI " + abi);
                }
            }
            // Android rename replaces atomically. Do not unlink a file another process may be loading.
            if (!temporary.renameTo(target) && !IntegrityVerifier.matchesShell(target, asset))
                throw new IOException("Cannot publish shell SO: " + target);
            return target.getAbsolutePath();
        } catch (IOException error) {
            throw new IllegalStateException("Cannot extract shell SO: ABI=" + abi +
                    ", asset=assets/" + asset + ", target=" + target, error);
        } finally {
            if (temporary != null && temporary.exists()) temporary.delete();
        }
    }
}
