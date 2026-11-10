package com.wpf.util.jiagulibrary.utils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.*;
import java.util.*;
import java.util.zip.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Shared streaming payload codec. No Android dependency: identical host and ART validation. */
public final class ResourceArchive {
    public static final String KEY_HEX = "";
    public static final String ASSET = "assets/jiagu-resources.bin";
    private static final int MAGIC = 0x4a475231;
    private static final long MAX_BYTES = 2L * 1024 * 1024 * 1024;

    public static final class Restored {
        public final File file;
        public final boolean cacheHit;
        Restored(File file, boolean cacheHit) { this.file = file; this.cacheHit = cacheHit; }
    }

    private static byte[] key(String hex) {
        if (hex.length() != 32) throw new IllegalArgumentException("Missing resource key");
        byte[] bytes = new byte[16];
        for (int i = 0; i < bytes.length; i++) {
            int high = Character.digit(hex.charAt(i * 2), 16), low = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) throw new IllegalArgumentException("Invalid resource key");
            bytes[i] = (byte) ((high << 4) | low);
        }
        return bytes;
    }
    private static Cipher cipher(int mode, String key, byte[] header, int index) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = Arrays.copyOfRange(header, 4, 16);
        for (int i = 0; i < 4; i++) iv[8 + i] ^= (byte) (index >>> (24 - i * 8));
        cipher.init(mode, new SecretKeySpec(key(key), "AES"), new GCMParameterSpec(128, iv));
        cipher.updateAAD(header);
        cipher.updateAAD(new byte[]{(byte)(index >>> 24), (byte)(index >>> 16), (byte)(index >>> 8), (byte)index});
        return cipher;
    }
    public static void encrypt(File plain, File base, File encrypted, String key) throws IOException, GeneralSecurityException {
        if (plain.length() <= 0 || plain.length() > MAX_BYTES) throw new IOException("Resource archive exceeds 2 GiB");
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(header);
        data.writeInt(MAGIC); byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);
        File reference = File.createTempFile("resource-reference-", ".apk", plain.getParentFile());
        byte[] expected;
        try { compose(base, plain, reference); expected = contentsDigest(reference); }
        finally { reference.delete(); }
        data.write(iv); data.writeLong(plain.length()); data.write(expected); data.close();
        try (InputStream in = new FileInputStream(plain); OutputStream out = new BufferedOutputStream(new FileOutputStream(encrypted))) {
            out.write(header.toByteArray()); transform(Cipher.ENCRYPT_MODE, key, header.toByteArray(), plain.length(), in, out);
        }
    }
    static void transform(int mode, String key, byte[] header, long length, InputStream input, OutputStream out) throws IOException, GeneralSecurityException {
        DataInputStream in = new DataInputStream(input);
        int index = 0;
        for (long remaining = length; remaining > 0; index++) {
            int plain = (int) Math.min(65536, remaining);
            byte[] block = new byte[plain + (mode == Cipher.DECRYPT_MODE ? 16 : 0)]; in.readFully(block);
            out.write(cipher(mode, key, header, index).doFinal(block)); remaining -= plain;
        }
        if (in.read() != -1) throw new IOException("Trailing resource payload bytes");
    }

    public static Restored restore(File apk, File directory, String version, String key) throws IOException, GeneralSecurityException {
        if (!version.matches("[0-9a-f]{64}")) throw new IOException("Invalid resource cache version");
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) throw new IOException("Cannot create resource cache");
        try (ZipFile zip = new ZipFile(apk)) {
            ZipEntry payload = zip.getEntry(ASSET);
            if (payload == null) return null;
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(zip.getInputStream(payload)));
                 FileOutputStream lockFile = new FileOutputStream(new File(directory, "resources.lock"), true);
                 FileChannel channel = lockFile.getChannel(); FileLock ignored = channel.lock()) {
                byte[] header = new byte[56]; in.readFully(header);
                DataInputStream fields = new DataInputStream(new ByteArrayInputStream(header));
                if (fields.readInt() != MAGIC) throw new IOException("Unknown resource payload version");
                fields.skipBytes(12); long length = fields.readLong(); byte[] hash = new byte[32]; fields.readFully(hash);
                if (length <= 0 || length > MAX_BYTES || payload.getSize() != 56 + length + ((length + 65535) / 65536) * 16)
                    throw new IOException("Invalid resource payload length");
                File target = new File(directory, "resources-" + version + ".apk");
                if (matches(target, hash)) return new Restored(target, true);
                File staged = File.createTempFile("resource-", ".tmp", directory);
                File selected = File.createTempFile("selected-", ".tmp", directory);
                try {
                    try (FileOutputStream output = new FileOutputStream(selected)) {
                        transform(Cipher.DECRYPT_MODE, key, header, length, in, output);
                        output.getFD().sync();
                    }
                    compose(apk, selected, staged);
                    if (!matches(staged, hash)) throw new GeneralSecurityException("Decrypted resource digest mismatch");
                    // Validate the ZIP before publishing anything the framework may map.
                    try (ZipFile restored = new ZipFile(staged)) {
                        if (restored.getEntry("AndroidManifest.xml") == null) throw new IOException("Resource archive has no Manifest");
                    }
                    if (!staged.setReadOnly()) throw new IOException("Cannot protect resource cache permissions");
                    if (target.exists()) {
                        target.setWritable(true, true);
                        if (!target.delete()) throw new IOException("Cannot replace damaged resource cache");
                    }
                    if (!staged.renameTo(target)) throw new IOException("Cannot publish decrypted resources");
                    return new Restored(target, false);
                } finally { selected.delete(); if (staged.exists()) { staged.setWritable(true, true); staged.delete(); } }
            }
        }
    }
    private static boolean matches(File file, byte[] hash) throws IOException, GeneralSecurityException {
        if (!file.isFile()) return false;
        try { return MessageDigest.isEqual(hash, contentsDigest(file)); }
        catch (ZipException | EOFException invalid) { return false; }
    }
    private static byte[] contentsDigest(File file) throws IOException, GeneralSecurityException {
        MessageDigest total = MessageDigest.getInstance("SHA-256");
        try (ZipFile zip = new ZipFile(file); DataOutputStream out = new DataOutputStream(new DigestOutputStream(new ByteArrayOutputStream(), total))) {
            List<String> names = new ArrayList<>(); Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) names.add(entries.nextElement().getName());
            if (names.size() > 100000 || new HashSet<>(names).size() != names.size()) throw new ZipException("Invalid resource cache entries");
            Collections.sort(names); long bytes = 0;
            for (String name : names) {
                ZipEntry entry = zip.getEntry(name); bytes += entry.getSize();
                if (entry.getSize() < 0 || bytes > MAX_BYTES) throw new ZipException("Resource cache too large");
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                try (InputStream in = zip.getInputStream(entry)) { byte[] b = new byte[65536]; int n; long read = 0;
                    while ((n = in.read(b)) != -1) { read += n; if (read > entry.getSize()) throw new ZipException("Invalid resource entry size"); hash.update(b, 0, n); }
                    if (read != entry.getSize()) throw new ZipException("Truncated resource entry");
                }
                out.writeUTF(name); out.write(hash.digest());
            }
        }
        return total.digest();
    }
    private static final class Counted extends FilterOutputStream {
        long count;
        Counted(OutputStream out) { super(out); }
        @Override public void write(int value) throws IOException { out.write(value); count++; }
        @Override public void write(byte[] b, int offset, int length) throws IOException { out.write(b, offset, length); count += length; }
    }
    private static void compose(File base, File selected, File output) throws IOException {
        try (ZipFile original = new ZipFile(base); ZipFile decrypted = new ZipFile(selected)) {
            Counted counter = new Counted(new BufferedOutputStream(new FileOutputStream(output)));
            try (ZipOutputStream out = new ZipOutputStream(counter)) {
            Map<String, ZipFile> sources = new TreeMap<>();
            Enumeration<? extends ZipEntry> entries = original.entries();
            while (entries.hasMoreElements()) { ZipEntry entry = entries.nextElement(); String name = entry.getName();
                if (!entry.isDirectory() && (name.startsWith("res/") || name.equals("resources.arsc") || name.equals("AndroidManifest.xml"))) sources.put(name, original);
            }
            entries = decrypted.entries();
            while (entries.hasMoreElements()) { ZipEntry entry = entries.nextElement(); String name = entry.getName();
                if (entry.isDirectory() || (!name.startsWith("res/") && !name.startsWith("assets/"))) throw new ZipException("Invalid encrypted resource path");
                sources.put(name, decrypted);
            }
            for (Map.Entry<String, ZipFile> source : sources.entrySet()) {
                ZipEntry from = source.getValue().getEntry(source.getKey()); ZipEntry target = new ZipEntry(from.getName());
                target.setMethod(ZipEntry.STORED); target.setSize(from.getSize()); target.setCrc(from.getCrc());
                int pad = (int)((4 - (counter.count + 30 + target.getName().getBytes("UTF-8").length + 4) % 4) % 4);
                byte[] extra = new byte[4 + pad]; extra[0] = (byte)0xfe; extra[1] = (byte)0xca; extra[2] = (byte)pad; target.setExtra(extra);
                out.putNextEntry(target);
                try (InputStream in = source.getValue().getInputStream(from)) { byte[] b = new byte[65536]; int n; while ((n = in.read(b)) != -1) out.write(b, 0, n); }
                out.closeEntry();
            }
            }
        }
    }
}
