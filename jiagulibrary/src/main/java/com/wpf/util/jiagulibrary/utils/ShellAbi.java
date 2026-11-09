package com.wpf.util.jiagulibrary.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Select the shell for the running ART process, including native-bridge emulators. */
final class ShellAbi {
    private ShellAbi() {}

    static String detect() {
        // Device ABI lists can differ from the ISA of the actual ART process.
        try (InputStream input = new FileInputStream("/proc/self/exe")) {
            byte[] header = new byte[20];
            new java.io.DataInputStream(input).readFully(header);
            return fromElfHeader(header);
        } catch (IOException error) {
            String arch = System.getProperty("os.arch", "");
            if ("x86_64".equals(arch) || "amd64".equals(arch)) return "x86_64";
            if ("x86".equals(arch) || arch.matches("i[3-6]86")) return "x86";
            if ("aarch64".equals(arch) || "arm64".equals(arch)) return "arm64-v8a";
            if (arch.startsWith("arm")) return "armeabi-v7a";
            throw new IllegalStateException("Cannot determine ART process ABI: " + arch, error);
        }
    }

    static String fromElfHeader(byte[] header) throws IOException {
        if (header.length < 20 || header[0] != 0x7f || header[1] != 'E' ||
                header[2] != 'L' || header[3] != 'F' || header[5] != 1) {
            throw new IOException("Invalid process ELF header");
        }
        int machine = (header[18] & 0xff) | ((header[19] & 0xff) << 8);
        if (header[4] == 1 && machine == 3) return "x86";
        if (header[4] == 2 && machine == 62) return "x86_64";
        if (header[4] == 1 && machine == 40) return "armeabi-v7a";
        if (header[4] == 2 && machine == 183) return "arm64-v8a";
        throw new IOException("Unsupported process ELF class/machine: " + header[4] + "/" + machine);
    }

    static String assetName(String abi) {
        switch (abi) {
            case "arm64-v8a": return "libjiagu_v8a.so";
            case "armeabi-v7a": return "libjiagu_v7a.so";
            case "x86": return "libjiagu_x86.so";
            case "x86_64": return "libjiagu_x86_64.so";
            default: throw new IllegalArgumentException("Unsupported shell ABI: " + abi);
        }
    }
}
