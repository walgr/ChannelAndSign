package com.wpf.jiagu.compat;

public class FixtureProvider extends android.content.ContentProvider {
    private static android.content.BroadcastReceiver earlyReceiver;
    private static android.content.ServiceConnection earlyConnection;

    @Override public boolean onCreate() {
        if (android.os.Build.VERSION.SDK_INT >= 28 && !FixtureComponentFactory.providerDispatched)
            throw new AssertionError("Host provider factory skipped");
        FixtureChecks.requireValid();
        android.content.Context app = getContext().getApplicationContext();
        if (!(app instanceof FixtureApplication) || app.getApplicationContext() != app)
            throw new AssertionError("Provider application context must be the original Application");
        if (FixtureApplication.onCreateCalled)
            throw new AssertionError("Application.onCreate ran before provider initialization");
        android.content.Context configured = getContext().createConfigurationContext(
                new android.content.res.Configuration(getContext().getResources().getConfiguration()));
        if (configured.getApplicationContext() != app)
            throw new AssertionError("Provider configuration context returned a different Application");
        earlyReceiver = new android.content.BroadcastReceiver() {
            @Override public void onReceive(android.content.Context context, android.content.Intent intent) { }
        };
        android.content.IntentFilter filter = new android.content.IntentFilter(app.getPackageName() + ".EARLY_PROBE");
        if (android.os.Build.VERSION.SDK_INT >= 33) app.registerReceiver(earlyReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
        else app.registerReceiver(earlyReceiver, filter);
        earlyConnection = new android.content.ServiceConnection() {
            @Override public void onServiceConnected(android.content.ComponentName name, android.os.IBinder service) { }
            @Override public void onServiceDisconnected(android.content.ComponentName name) { }
        };
        if (!app.bindService(new android.content.Intent(app, FixtureService.class), earlyConnection, android.content.Context.BIND_AUTO_CREATE))
            throw new AssertionError("Early service binding failed");
        android.util.Log.i("JIAGU_COMPAT", "COMPAT_PROVIDER_PASSED pid=" + android.os.Process.myPid()); return true;
    }
    static void checkEarlyCallbacks(android.content.Context app) {
        if (earlyConnection == null) return; // Remote process has its own provider.
        // Reusing the same connection also validates ServiceDispatcher.mContext.
        if (!app.bindService(new android.content.Intent(app, FixtureService.class), earlyConnection, android.content.Context.BIND_AUTO_CREATE))
            throw new AssertionError("Delegate service binding failed");
        app.unbindService(earlyConnection);
        app.unregisterReceiver(earlyReceiver);
        earlyConnection = null; earlyReceiver = null;
        android.util.Log.i("JIAGU_COMPAT", "COMPAT_EARLY_CALLBACKS_PASSED");
    }
    @Override public android.database.Cursor query(android.net.Uri uri, String[] projection, String selection, String[] args, String order) {
        FixtureChecks.requireValid(); android.database.MatrixCursor cursor = new android.database.MatrixCursor(new String[]{"probe"});
        cursor.addRow(new Object[]{"ok"}); return cursor;
    }
    @Override public String getType(android.net.Uri uri) { return "vnd.android.cursor.item/probe"; }
    @Override public android.net.Uri insert(android.net.Uri uri, android.content.ContentValues values) { return null; }
    @Override public int delete(android.net.Uri uri, String selection, String[] args) { return 0; }
    @Override public int update(android.net.Uri uri, android.content.ContentValues values, String selection, String[] args) { return 0; }
}
