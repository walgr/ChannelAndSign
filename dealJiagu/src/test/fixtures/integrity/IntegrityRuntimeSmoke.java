package com.wpf.jiagu.integrity;

import com.wpf.util.jiagulibrary.utils.IntegrityVerifier;
import java.io.*;
import java.util.zip.*;

/** Invoked with dalvikvm/CheckJNI; never installs or modifies an application. */
public class IntegrityRuntimeSmoke {
    public static void main(String[] args) throws Exception {
        File apk = new File(args[0]);
        IntegrityVerifier.verify(apk, args[1]);
        File extracted = new File(args[2]);
        try {
            try (ZipFile zip = new ZipFile(apk);
                 InputStream in = zip.getInputStream(zip.getEntry("assets/libjiagu_x86_64.so"));
                 FileOutputStream out = new FileOutputStream(extracted)) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            }
            if (!IntegrityVerifier.matchesShell(extracted, "libjiagu_x86_64.so")) throw new AssertionError("valid extraction rejected");
            try (RandomAccessFile file = new RandomAccessFile(extracted, "rw")) {
                file.seek(100); int original = file.readUnsignedByte(); file.seek(100); file.write(original ^ 1);
            }
            if (IntegrityVerifier.matchesShell(extracted, "libjiagu_x86_64.so")) throw new AssertionError("damaged extraction accepted");
        } finally { extracted.delete(); }
        System.out.println("ANDROID_INTEGRITY_PASSED: signed APK content and damaged cached SO detection");
    }
}
