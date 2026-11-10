package com.wpf.jiagu.compat;

public class FixtureProvider extends android.content.ContentProvider {
    @Override public boolean onCreate() {
        FixtureChecks.requireValid();
        android.util.Log.i("JIAGU_COMPAT", "COMPAT_PROVIDER_PASSED pid=" + android.os.Process.myPid()); return true;
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
