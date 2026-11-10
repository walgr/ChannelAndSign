package com.wpf.jiagu.d2c;

public class SmokeActivity extends android.app.Activity {
    private int restorationProbe(int value) { return value + 37; }

    @Override protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        try {
            D2cSmoke.main(new String[] { "shell" });
            if (restorationProbe(5) != 42) throw new AssertionError("method restoration failed");
            android.widget.TextView text = new android.widget.TextView(this);
            text.setText("HYBRID_APK_PASSED\nMethod restoration + D2C + VMP\n100 input combinations");
            setContentView(text);
            android.util.Log.i("HYBRID_SMOKE", "HYBRID_APK_PASSED");
        } catch (Throwable error) {
            android.util.Log.e("HYBRID_SMOKE", "HYBRID_APK_FAILED", error);
            throw new RuntimeException(error);
        }
    }
}
