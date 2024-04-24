package com.wpf.util.jiagulibrary.utils;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by linchaolong on 2015/10/28.
 */
public class IO {

    public static final String TAG = IO.class.getSimpleName();

    public static final void close(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] getInputStreamData(InputStream inputStream) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(Math.max(8 * 1024, inputStream.available()));
            copyTo(inputStream, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static long copyTo(InputStream inputStream, OutputStream out) {
        try {
            long bytesCopied = 0;
            byte[] buffer = new byte[8 * 1024];
            int bytes = inputStream.read(buffer);
            while (bytes >= 0) {
                out.write(buffer, 0, bytes);
                bytesCopied += bytes;
                bytes = inputStream.read(buffer);
            }
            return bytesCopied;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 直接从apk中读取dex数据
     *
     * @param zip
     * @return
     */
    public static byte[] getDexData(String zip) {
        ZipFile zf = null;
        ZipEntry entry;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            zf = new ZipFile(zip);
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                if (entry.getName().indexOf("/") > 0) {
                    continue;
                }
                if (!entry.isDirectory()) {
                    if ("classes.dex".equals(entry.getName())) {
                        InputStream is = zf.getInputStream(entry);
                        int len = 0;
                        byte[] bytes = new byte[1024 * 8];
                        while ((len = is.read(bytes)) != -1) {
                            byteArrayOutputStream.write(bytes, 0, len);
                        }
                        is.close();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (zf != null) {
                try {
                    zf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                byteArrayOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return byteArrayOutputStream.toByteArray();
    }
}
