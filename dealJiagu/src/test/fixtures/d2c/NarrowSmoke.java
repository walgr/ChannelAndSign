package com.wpf.jiagu.d2c;

public class NarrowSmoke {
    public static byte asByte(int x) { return (byte)x; }
    public static short asShort(int x) { return (short)x; }
    public static char asChar(int x) { return (char)x; }
    public static int mix(byte a, short b, char c) { return a * 31 + b - c; }
    public static boolean choose(boolean a, boolean b) { return a && !b; }
    public static short narrowLoop(int x) { short s = 0; for (int i = 0; i < (x & 15); i++) s = (short)(s + x); return s; }
    public static int conversions(int x) { return (byte)x + (short)x - (char)x; }

    public static void main(String[] args) throws Exception {
        boolean nativeExpected = args.length > 0;
        if (nativeExpected) System.load(args[0]);
        if (java.lang.reflect.Modifier.isNative(NarrowSmoke.class.getDeclaredMethod("asByte", int.class).getModifiers()) != nativeExpected)
            throw new AssertionError("native declaration mismatch");
        int[] values = {0, 1, -1, -129, -128, 127, 128, 255, 256, -32769, -32768, 32767, 32768, 65535, 65536, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (int x : values) {
            check(asByte(x) == (byte)x); check(asShort(x) == (short)x); check(asChar(x) == (char)x);
            check(mix((byte)x, (short)x, (char)x) == (byte)x * 31 + (short)x - (char)x);
            short result = 0; for (int i = 0; i < (x & 15); i++) result = (short)(result + x);
            check(narrowLoop(x) == result);
            check(conversions(x) == (byte)x + (short)x - (char)x);
        }
        for (boolean a : new boolean[]{false, true}) for (boolean b : new boolean[]{false, true}) check(choose(a,b) == (a && !b));
        System.out.println("NARROW_SMOKE_PASSED native=" + nativeExpected + " values=" + values.length);
    }
    private static void check(boolean ok) { if (!ok) throw new AssertionError("narrow scalar mismatch"); }
}
