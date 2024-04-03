package com.wpf.util.jiagulibrary.utils;

import java.io.*;

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
}
