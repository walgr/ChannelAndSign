package com.wpf.jiagu.compat;

public class FixtureActivity extends android.app.Activity {
    @Override public void onCreate(android.os.Bundle state) {
        super.onCreate(state); FixtureChecks.requireValid();
        if (!(getApplicationContext() instanceof FixtureApplication)) throw new AssertionError("Original Application context missing");
        try (android.database.Cursor cursor = getContentResolver().query(android.net.Uri.parse("content://" + getPackageName() + ".main/probe"), null, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst() || !"ok".equals(cursor.getString(0))) throw new AssertionError("Provider failed");
        }
        int layout = getResources().getIdentifier("resource_probe", "layout", getPackageName());
        android.widget.TextView text = (android.widget.TextView) getLayoutInflater().inflate(layout, null);
        if (!"资源保护 ✓".contentEquals(text.getText()) || text.getBackground() == null)
            throw new AssertionError("Activity encrypted layout/image failed");
        setContentView(text);
        String token = getIntent().getStringExtra("matrix_run");
        if (token != null) {
            String expected = getIntent().getStringExtra("matrix_state");
            android.content.SharedPreferences prefs = getSharedPreferences("matrix", MODE_PRIVATE);
            String saved = prefs.getString("sentinel", null);
            if (saved == null) {
                if (getIntent().getBooleanExtra("matrix_require_state", false)) throw new AssertionError("Update lost persistent state");
                if (!prefs.edit().putString("sentinel", expected).commit()) throw new AssertionError("State write failed");
            } else if (!saved.equals(expected)) throw new AssertionError("Update lost persistent state");
            try {
                if (getPackageManager().getPackageInfo(getPackageName(), 0).versionCode != getIntent().getIntExtra("matrix_version", 1))
                    throw new AssertionError("Unexpected installed version");
            } catch (android.content.pm.PackageManager.NameNotFoundException error) { throw new AssertionError(error); }
            for (int i = 0; i < 1000; i++) FixtureChecks.requireValid();
            if (getIntent().getBooleanExtra("matrix_recreate", false) && state == null) { recreate(); return; }
            android.util.Log.i("JIAGU_COMPAT", "MATRIX_ACTIVITY_PASSED token=" + token + " pid=" + android.os.Process.myPid());
        }
        android.util.Log.i("JIAGU_COMPAT", "COMPAT_ACTIVITY_PASSED pid=" + android.os.Process.myPid());
    }
}
