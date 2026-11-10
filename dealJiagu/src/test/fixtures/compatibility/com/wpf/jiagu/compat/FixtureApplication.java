package com.wpf.jiagu.compat;

public class FixtureApplication extends android.app.Application {
    @Override public void onCreate() {
        super.onCreate(); FixtureChecks.requireValid();
        android.util.Log.i("JIAGU_COMPAT", "COMPAT_APPLICATION_PASSED pid=" + android.os.Process.myPid());
    }
}
