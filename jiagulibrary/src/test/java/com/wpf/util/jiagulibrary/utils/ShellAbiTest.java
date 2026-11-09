package com.wpf.util.jiagulibrary.utils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/** Plain Java regression test; run with assertions enabled (-ea). */
public final class ShellAbiTest {
    private static byte[] header(int bits, int machine) {
        byte[] header = new byte[20];
        header[0] = 0x7f; header[1] = 'E'; header[2] = 'L'; header[3] = 'F';
        header[4] = (byte) (bits == 64 ? 2 : 1); header[5] = 1;
        header[18] = (byte) machine;
        return header;
    }

    public static void main(String[] args) throws Exception {
        assert ShellAbi.fromElfHeader(header(32, 3)).equals("x86");
        assert ShellAbi.fromElfHeader(header(64, 62)).equals("x86_64");
        assert ShellAbi.fromElfHeader(header(32, 40)).equals("armeabi-v7a");
        assert ShellAbi.fromElfHeader(header(64, 183)).equals("arm64-v8a");
        for (byte[] invalid : new byte[][] {new byte[0], header(32, 62), header(64, 3), header(64, 255)}) {
            try {
                ShellAbi.fromElfHeader(invalid);
                throw new AssertionError("Accepted invalid ELF");
            } catch (IOException expected) { /* expected */ }
        }
        String[] abis = {"x86", "x86_64", "armeabi-v7a", "arm64-v8a"};
        for (String abi : abis) {
            File file = new File(args[0], ShellAbi.assetName(abi));
            try (DataInputStream input = new DataInputStream(new FileInputStream(file))) {
                byte[] header = new byte[20];
                input.readFully(header);
                assert ShellAbi.fromElfHeader(header).equals(abi) : file;
            }
        }
        System.out.println("Shell ABI tests passed (including all four cached SO ELF headers)");
    }
}
