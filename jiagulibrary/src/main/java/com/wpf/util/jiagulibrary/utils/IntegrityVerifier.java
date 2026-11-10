package com.wpf.util.jiagulibrary.utils;

import java.io.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.zip.*;

/** Runs before loading any shell native code. Public key is injected by the packager. */
public final class IntegrityVerifier {
    public static final String PUBLIC_KEY_HEX = "";
    public static final String RECORD = "assets/jiagu-integrity.bin";
    public static final String SIGNATURE = "assets/jiagu-integrity.sig";
    private static volatile Map<String, byte[]> verified = Collections.emptyMap();

    public static void verify(File apk) { verify(apk, PUBLIC_KEY_HEX); }

    public static synchronized void verify(File apk, String publicKeyHex) {
        verified = Collections.emptyMap();
        try (ZipFile zip = new ZipFile(apk)) {
            Set<String> names = new HashSet<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!names.add(name)) throw new IOException("Duplicate APK entry: " + name);
            }
            byte[] record = readSmall(zip, RECORD, 32 * 1024 * 1024);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(hex(publicKeyHex))));
            signature.update(record);
            if (!signature.verify(readSmall(zip, SIGNATURE, 512))) throw new GeneralSecurityException("Invalid integrity signature");
            Map<String, byte[]> hashes = new HashMap<>();
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(record))) {
                if (in.readInt() != 0x4a474931) throw new IOException("Unknown integrity record version");
                int count = in.readInt();
                if (count < 3 || count > 100000) throw new IOException("Invalid integrity entry count");
                for (int i = 0; i < count; i++) {
                    String name = in.readUTF();
                    if (name.endsWith("/") || !isProtected(name) || hashes.containsKey(name)) throw new IOException("Invalid integrity entry: " + name);
                    byte[] expected = new byte[32]; in.readFully(expected);
                    ZipEntry entry = zip.getEntry(name);
                    if (entry == null) throw new IOException("Missing protected entry: " + name);
                    try (InputStream stream = zip.getInputStream(entry)) {
                        if (!MessageDigest.isEqual(expected, digest(stream))) throw new GeneralSecurityException("APK integrity mismatch: " + name);
                    }
                    hashes.put(name, expected);
                }
                if (in.available() != 0 || !hashes.containsKey("classes.dex") || !hashes.containsKey("AndroidManifest.xml"))
                    throw new IOException("Incomplete integrity record");
            }
            for (String name : names) if (!name.endsWith("/") && isProtected(name) && !hashes.containsKey(name))
                throw new IOException("Unrecorded protected entry: " + name);
            verified = Collections.unmodifiableMap(hashes);
        } catch (IOException | GeneralSecurityException | IllegalArgumentException error) {
            throw new IllegalStateException("Shell integrity verification failed: " + error.getMessage(), error);
        }
    }

    public static boolean matchesShell(File file, String asset) {
        byte[] expected = verified.get("assets/" + asset);
        if (expected == null) throw new IllegalStateException("Shell asset was not verified: " + asset);
        if (!file.isFile()) return false;
        try (InputStream input = new FileInputStream(file)) { return MessageDigest.isEqual(expected, digest(input)); }
        catch (IOException | GeneralSecurityException error) { return false; }
    }

    public static String resourceVersion() {
        byte[] hash = verified.get(ResourceArchive.ASSET);
        if (hash == null) return null;
        StringBuilder hex = new StringBuilder(64);
        for (byte value : hash) hex.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return hex.toString();
    }

    public static String nativeVersion() {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            List<String> names = new ArrayList<>();
            for (String name : verified.keySet()) if (name.startsWith(NativeArchive.PREFIX)) names.add(name);
            if (names.isEmpty()) return null;
            Collections.sort(names);
            for (String name : names) { hash.update(name.getBytes("UTF-8")); hash.update((byte)0); hash.update(verified.get(name)); }
            StringBuilder result = new StringBuilder();
            for (byte b : hash.digest()) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return result.toString();
        } catch (GeneralSecurityException | IOException error) { throw new IllegalStateException(error); }
    }

    private static boolean isProtected(String name) {
        return "classes.dex".equals(name) || "AndroidManifest.xml".equals(name) ||
            "resources.arsc".equals(name) || name.startsWith("res/") || name.matches("lib/[^/]+/[^/]+\\.so") ||
            (name.startsWith("assets/") && !RECORD.equals(name) && !SIGNATURE.equals(name));
    }

    private static byte[] readSmall(ZipFile zip, String name, int limit) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) throw new IOException("Missing integrity record: " + name);
        try (InputStream in = zip.getInputStream(entry); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024]; int count;
            while ((count = in.read(buffer)) != -1) {
                if (out.size() + count > limit) throw new IOException("Integrity record too large");
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        }
    }

    private static byte[] digest(InputStream in) throws IOException, GeneralSecurityException {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[65536]; int count;
        while ((count = in.read(buffer)) != -1) hash.update(buffer, 0, count);
        return hash.digest();
    }

    private static byte[] hex(String value) {
        if (value.length() < 512 || (value.length() & 1) != 0) throw new IllegalArgumentException("Missing integrity public key");
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int a = Character.digit(value.charAt(i * 2), 16), b = Character.digit(value.charAt(i * 2 + 1), 16);
            if (a < 0 || b < 0) throw new IllegalArgumentException("Invalid public key encoding");
            result[i] = (byte) ((a << 4) | b);
        }
        return result;
    }
}
