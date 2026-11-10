package com.wpf.jiagu.d2c;

public class D2cSmoke {
    public static int mix(int a, int b) {
        int c = (a * 31 + b) ^ (a >>> (b & 31));
        if (c < 0) c = -c;
        return (c >> (a & 31)) + (b << 5) - (~a);
    }
    public static int divide(int a, int b) { return a / b; }
    public static int remainder(int a, int b) { return a % b; }
    public static int literal(int a) { return ((123 - a) * -7) ^ (a >> 11); }
    public static int loop(int a) {
        int result = 0;
        for (int i = 0; i < (a & 15); i++) result += i;
        return result;
    }
    public static int overloaded(int a) { return a + 1; }
    public static int overloaded(int a, int b) { return a - b; }
    public static int under_score(int a) { return a & 127; }
    public static int bitwise(int a, int b) { return (a | b) ^ (a & b) ^ (a << b) ^ (a >> b) ^ (a >>> b) ^ 0x7fff0000; }
    public static int compare(int a, int b) { if (a == b) return 1; if (a < b) return 2; if (a > b) return 3; return 4; }
    public static int zeroCompare(int a) { if (a == 0) return 4; if (a < 0) return 5; return 6; }
    public static int unsupported(int a) { return Integer.parseInt(String.valueOf(a)); }
    public static int catchBlock(int a) { try { return 100 / a; } catch (ArithmeticException e) { return 0; } }
    public static int mainProcess(int a) { return a + 2; } // Left in DEX as a negative selection check.

    public static void main(String[] args) throws Exception {
        boolean expectNative = args.length != 0;
        if (expectNative && !"shell".equals(args[0])) System.load(args[0]);
        if (java.lang.reflect.Modifier.isNative(D2cSmoke.class.getDeclaredMethod("mix", int.class, int.class).getModifiers()) != expectNative)
            throw new AssertionError("native declaration mismatch");
        int[] values = {0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, 31, 32, -32, 1234567, -8765432};
        int cases = 0;
        for (int a : values) for (int b : values) {
            int c = (a * 31 + b) ^ (a >>> (b & 31));
            if (c < 0) c = -c;
            check(mix(a, b), (c >> (a & 31)) + (b << 5) - (~a));
            if (b != 0) { check(divide(a, b), a / b); check(remainder(a, b), a % b); }
            check(literal(a), ((123 - a) * -7) ^ (a >> 11));
            int sum = 0; for (int i = 0; i < (a & 15); i++) sum += i;
            check(loop(a), sum);
            check(overloaded(a), a + 1); check(overloaded(a, b), a - b);
            check(under_score(a), a & 127); check(mainProcess(a), a + 2);
            check(bitwise(a, b), (a | b) ^ (a & b) ^ (a << b) ^ (a >> b) ^ (a >>> b) ^ 0x7fff0000);
            check(compare(a, b), a == b ? 1 : a < b ? 2 : a > b ? 3 : 4);
            check(zeroCompare(a), a == 0 ? 4 : a < 0 ? 5 : 6);
            cases++;
        }
        try { divide(1, 0); throw new AssertionError("missing divide exception"); } catch (ArithmeticException expected) { }
        try { remainder(1, 0); throw new AssertionError("missing remainder exception"); } catch (ArithmeticException expected) { }
        System.out.println("D2C_SMOKE_PASSED native=" + expectNative + " cases=" + cases);
    }
    private static void check(int a, int b) { if (a != b) throw new AssertionError(a + " != " + b); }
}
