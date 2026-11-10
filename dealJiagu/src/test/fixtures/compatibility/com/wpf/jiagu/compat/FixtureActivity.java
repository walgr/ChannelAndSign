package com.wpf.jiagu.compat;

public class FixtureActivity extends android.app.Activity {
    @Override public void onCreate(android.os.Bundle state) {
        super.onCreate(state); FixtureChecks.requireValid();
        if (!(getApplicationContext() instanceof FixtureApplication)) throw new AssertionError("Original Application context missing");
        try (android.database.Cursor cursor = getContentResolver().query(android.net.Uri.parse("content://com.wpf.jiagu.compat.main/probe"), null, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst() || !"ok".equals(cursor.getString(0))) throw new AssertionError("Provider failed");
        }
        android.widget.TextView text = new android.widget.TextView(this); text.setText("Compatibility regression passed"); setContentView(text);
        android.util.Log.i("JIAGU_COMPAT", "COMPAT_ACTIVITY_PASSED pid=" + android.os.Process.myPid());
    }
}
