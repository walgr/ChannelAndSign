package com.wpf.util.jiagulibrary.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import com.wpf.util.jiagulibrary.StubApp;

import java.io.*;
import java.util.Objects;

public class AssetsUtil {
    public static String copyJiaGu(Context context) {
        String absolutePath = Objects.requireNonNull(context.getFilesDir().getParentFile()).getAbsolutePath();
        File jiaGuDir = new File(absolutePath, ".jiagu");
        if (jiaGuDir.exists()) {
            boolean result = jiaGuDir.delete();
        }
        boolean result = jiaGuDir.mkdir();

        String destSo = absolutePath + "/.jiagu/libjiagu" + StubApp.VERSION +".so";

        boolean is64 = Build.CPU_ABI.contains("64") || Build.CPU_ABI2.contains("64");
        String soName = is64 ? "libjiagu_64.so" : "libjiagu.so";

        if ("x86".equals(Build.CPU_ABI)) {
            soName = "libjiagu_x86.so";
        } else if ("x86_64".equals(Build.CPU_ABI)) {
            soName = "libjiagu_x86_64.so";
        }

        Log.w("NDK_JIAGU", "soName:" + soName);


        writeFile(context, soName, destSo);

        return destSo;
    }

    private static void writeFile(Context context, String in, String out) {
        File outFile = new File(out);
        if (outFile.exists()) {
            return;
        }

        File jiaGuDir = outFile.getParentFile();
        if (jiaGuDir != null) {
            File[] files = jiaGuDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    boolean result = file.delete();
                    if (!result) continue;
                }
            }
        }

        try (InputStream is = context.getAssets().open(in); OutputStream os = new FileOutputStream(out)) {
            byte[] buffer = new byte[1024 * 8];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        } catch (IOException ignore) {
        }
    }
}
