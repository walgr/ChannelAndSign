package com.wpf.jiagu.compat;

public class FixtureChecks {
    public static int straight(int x) { return (x * 7) ^ 0x1234; }
    public static int branching(int x) { int sum = 0; for (int i = 0; i < (x & 15); i++) sum += i; return sum; }
    public int restored(int x) { return x * 3 + 1; }
    public static void requireValid() {
        try {
            if (!java.lang.reflect.Modifier.isNative(FixtureChecks.class.getDeclaredMethod("straight", int.class).getModifiers()))
                throw new AssertionError("D2C was not applied");
        } catch (NoSuchMethodException error) { throw new AssertionError(error); }
        if (straight(123) != ((123 * 7) ^ 0x1234) || branching(13) != 78 || new FixtureChecks().restored(7) != 22 ||
                new SecondaryChecks().value(7) != 37)
            throw new AssertionError("D2C/VMP/restoration mismatch");
    }
}
