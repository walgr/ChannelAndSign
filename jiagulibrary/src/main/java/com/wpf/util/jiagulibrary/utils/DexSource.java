package com.wpf.util.jiagulibrary.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.security.NoSuchAlgorithmException;

/** Streams the protected DEX to native memory without a DEX-sized Java array. */
public final class DexSource implements Closeable {
    public final long length;
    private final ZipFile archive;
    private final InputStream input;
    private final MessageDigest digest;
    private final byte[] expected;
    private boolean verified;

    public DexSource(String apk) throws IOException {
        this(apk, null);
    }

    public DexSource(String apk, byte[] expectedDigest) throws IOException {
        expected = expectedDigest == null ? null : expectedDigest.clone();
        try { digest = expected == null ? null : MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException error) { throw new IOException(error); }
        archive = new ZipFile(apk);
        try {
            ZipEntry entry = archive.getEntry("classes.dex");
            if (entry == null || entry.isDirectory() || entry.getSize() < 4 || entry.getSize() > Integer.MAX_VALUE) {
                throw new IOException("Invalid protected classes.dex length");
            }
            length = entry.getSize();
            InputStream stream = archive.getInputStream(entry);
            input = digest == null ? stream : new DigestInputStream(stream, digest);
        } catch (IOException | RuntimeException error) {
            archive.close();
            throw error;
        }
    }

    public int read(byte[] buffer) throws IOException {
        int count = input.read(buffer);
        if (count == -1 && expected != null && !verified) {
            if (!MessageDigest.isEqual(expected, digest.digest())) throw new IOException("Protected DEX integrity mismatch");
            verified = true;
        }
        return count;
    }

    /** The signed index avoids allocating or scanning the whole merged DEX to find its footer. */
    public long startPayload() throws IOException {
        ZipEntry index = archive.getEntry("assets/jiagu-payload.bin");
        if (index == null || index.getSize() != 12) throw new IOException("Missing protected DEX index");
        long offset;
        try (DataInputStream data = new DataInputStream(archive.getInputStream(index))) {
            if (data.readInt() != 0x4a475031) throw new IOException("Invalid protected DEX index");
            offset = data.readInt() & 0xffffffffL;
            long size = data.readInt() & 0xffffffffL;
            if (size != length || offset < 112 || offset > length - 4 - 528) throw new IOException("Invalid protected DEX bounds");
        }
        byte[] buffer = new byte[8192];
        long toSkip = offset;
        while (toSkip > 0) {
            int count = input.read(buffer, 0, (int) Math.min(toSkip, buffer.length));
            if (count <= 0) throw new IOException("Truncated shell DEX");
            toSkip -= count;
        }
        // Return encrypted payload bytes, excluding the original shell and final offset word.
        return length - offset - 4;
    }

    @Override public void close() throws IOException {
        try {
            input.close();
            if (expected != null && !verified) throw new IOException("Protected DEX stream was not verified");
        } finally { archive.close(); }
    }
}
