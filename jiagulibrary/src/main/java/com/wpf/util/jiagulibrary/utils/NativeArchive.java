package com.wpf.util.jiagulibrary.utils;

import java.io.*;
import java.nio.channels.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;
import javax.crypto.Cipher;

/** Authenticated, bounded native payloads. Shared unchanged by the packager and Android. */
public final class NativeArchive {
    public static final String KEY_HEX = "";
    public static final String PREFIX = "assets/jiagu-native/";
    private static final int MAGIC = 0x4a474e31;
    private static final long LIMIT = 4L * 1024 * 1024;
    public static final class Restored {
        public final File directory;
        public final boolean cacheHit;
        Restored(File directory, boolean cacheHit) { this.directory = directory; this.cacheHit = cacheHit; }
    }
    private static byte[] digest(File file) throws IOException, GeneralSecurityException {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] b = new byte[65536]; int n;
            while ((n = in.read(b)) != -1) hash.update(b, 0, n);
        }
        return hash.digest();
    }
    public static void encrypt(File plain, File output, String key) throws IOException, GeneralSecurityException {
        if (plain.length() < 20 || plain.length() > LIMIT) throw new IOException("Invalid native payload size");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream fields = new DataOutputStream(bytes);
        fields.writeInt(MAGIC); byte[] nonce = new byte[12]; new SecureRandom().nextBytes(nonce);
        fields.write(nonce); fields.writeLong(plain.length()); fields.write(digest(plain)); fields.close();
        byte[] header = bytes.toByteArray();
        try (InputStream in = new FileInputStream(plain); OutputStream out = new BufferedOutputStream(new FileOutputStream(output))) {
            out.write(header); ResourceArchive.transform(Cipher.ENCRYPT_MODE, key, header, plain.length(), in, out);
        }
    }
    public static Restored restore(File apk, File root, String version, String abi, String key) throws IOException, GeneralSecurityException {
        if (!version.matches("[0-9a-f]{64}") || !Arrays.asList("x86", "x86_64", "armeabi-v7a", "arm64-v8a").contains(abi))
            throw new IOException("Invalid native cache identity");
        File directory = new File(new File(root, version), abi);
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) throw new IOException("Cannot create native cache");
        boolean hit = true; long total = 0; int count = 0;
        try (ZipFile zip = new ZipFile(apk); FileOutputStream lockFile = new FileOutputStream(new File(root, "native.lock"), true);
             FileChannel channel = lockFile.getChannel(); FileLock ignored = channel.lock()) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            String prefix = PREFIX + abi + "/";
            Set<String> names = new HashSet<>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith(prefix)) continue;
                String name = entry.getName().substring(prefix.length());
                if (!name.matches("lib[A-Za-z0-9_.+-]+\\.so\\.bin") || !names.add(name)) throw new IOException("Invalid native payload name");
                try (DataInputStream in = new DataInputStream(new BufferedInputStream(zip.getInputStream(entry)))) {
                    byte[] header = new byte[56]; in.readFully(header);
                    DataInputStream fields = new DataInputStream(new ByteArrayInputStream(header));
                    if (fields.readInt() != MAGIC) throw new IOException("Unknown native payload version");
                    fields.skipBytes(12); long length = fields.readLong(); byte[] hash = new byte[32]; fields.readFully(hash);
                    total += length; count++;
                    if (length < 20 || length > LIMIT || total > 16L * 1024 * 1024 || count > 1024 ||
                        entry.getSize() != 56 + length + ((length + 65535) / 65536) * 16) throw new IOException("Invalid native payload length");
                    File target = new File(directory, name.substring(0, name.length() - 4));
                    if (target.isFile() && target.length() == length && MessageDigest.isEqual(hash, digest(target))) continue;
                    hit = false;
                    File staged = File.createTempFile("native-", ".tmp", directory);
                    try {
                        try (FileOutputStream out = new FileOutputStream(staged)) {
                            ResourceArchive.transform(Cipher.DECRYPT_MODE, key, header, length, in, out); out.getFD().sync();
                        }
                        if (!MessageDigest.isEqual(hash, digest(staged))) throw new GeneralSecurityException("Native digest mismatch");
                        try (DataInputStream elf = new DataInputStream(new FileInputStream(staged))) {
                            byte[] h = new byte[20]; elf.readFully(h);
                            if (!abi.equals(ShellAbi.fromElfHeader(h))) throw new IOException("Native ELF ABI mismatch");
                        }
                        if (!staged.setReadOnly()) throw new IOException("Cannot protect native cache");
                        if (target.exists() && !target.delete()) throw new IOException("Cannot replace native cache");
                        if (!staged.renameTo(target)) throw new IOException("Cannot publish native cache");
                    } finally { if (staged.exists()) { staged.setWritable(true, true); staged.delete(); } }
                }
            }
        }
        return count == 0 ? null : new Restored(directory, hit);
    }
}
