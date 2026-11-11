package com.wpf.jiagu.compat;

public class FixtureService extends android.app.Service {
    @Override public android.os.IBinder onBind(android.content.Intent intent) { return new android.os.Binder(); }
}
