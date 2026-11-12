package com.wpf.jiagu.compat;

public class FixtureApplication extends android.app.Application {
    static boolean onCreateCalled;
    @Override protected void attachBaseContext(android.content.Context context) {
        super.attachBaseContext(context);
        if (android.os.Build.VERSION.SDK_INT >= 28 && !FixtureComponentFactory.applicationCreated)
            throw new AssertionError("Host component factory skipped before Application.attach");
        try {
            android.os.Bundle metadata = getPackageManager().getApplicationInfo(getPackageName(), android.content.pm.PackageManager.GET_META_DATA).metaData;
            FixtureChecks.expectHardened = metadata == null || metadata.getBoolean("fixture.hardened", true);
            if (metadata == null || metadata.getBoolean("fixture.native", true)) NativeChecks.requireValid();
            if (metadata != null && metadata.getBoolean("fixture.replaceLoader", false)) {
                try {
                    java.lang.reflect.Field info = context.getClass().getDeclaredField("mPackageInfo");
                    info.setAccessible(true);
                    Object loaded = info.get(context);
                    java.lang.reflect.Field loader = loaded.getClass().getDeclaredField("mClassLoader");
                    loader.setAccessible(true);
                    loader.set(loaded, new dalvik.system.PathClassLoader("", context.getClassLoader()));
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            }
        } catch (android.content.pm.PackageManager.NameNotFoundException error) { throw new AssertionError(error); }
    }
    @Override public void onCreate() {
        if (onCreateCalled) throw new AssertionError("Application.onCreate called twice");
        onCreateCalled = true;
        super.onCreate(); FixtureChecks.requireValid();
        FixtureProvider.checkEarlyCallbacks(this);
        checkResources();
        android.util.Log.i("JIAGU_COMPAT", "COMPAT_APPLICATION_PASSED pid=" + android.os.Process.myPid());
    }

    private void checkResources() {
        android.content.res.Resources res = getResources();
        int text = res.getIdentifier("resource_probe", "string", getPackageName());
        int layout = res.getIdentifier("resource_probe", "layout", getPackageName());
        int drawable = res.getIdentifier("resource_probe", "drawable", getPackageName());
        int raw = res.getIdentifier("resource_probe", "raw", getPackageName());
        if (!"资源保护 ✓".equals(res.getString(text))) throw new AssertionError("Resource string mismatch");
        android.widget.TextView view = (android.widget.TextView) android.view.LayoutInflater.from(this).inflate(layout, null);
        if (!res.getString(text).contentEquals(view.getText()) || res.getDrawable(drawable) == null)
            throw new AssertionError("Resource XML loading failed");
        int image = res.getIdentifier("business_image", "drawable", getPackageName());
        android.graphics.drawable.BitmapDrawable bitmap = (android.graphics.drawable.BitmapDrawable) res.getDrawable(image);
        if (bitmap.getBitmap().getWidth() != 4 || bitmap.getBitmap().getHeight() != 3 ||
                bitmap.getBitmap().getPixel(0, 0) != 0xff123456) throw new AssertionError("Encrypted image mismatch");
        android.content.Context configured = createConfigurationContext(new android.content.res.Configuration(res.getConfiguration()));
        if (configured.getResources().getDrawable(image) == null) throw new AssertionError("Configuration context resource failed");
        try (java.io.InputStream input = res.openRawResource(raw);
             java.io.InputStream asset = getAssets().open("resource_probe.txt")) {
            if (!"resource raw probe".equals(read(input)) || !"resource asset probe".equals(read(asset)))
                throw new AssertionError("Resource stream mismatch");
        } catch (java.io.IOException error) { throw new AssertionError(error); }
        try (java.io.InputStream large = getAssets().open("resource_large.bin")) {
            if (large.read() != 0 || large.read() != 1) throw new AssertionError("Unencrypted large asset fallback failed");
        } catch (java.io.IOException error) { throw new AssertionError(error); }
        try (android.content.res.AssetFileDescriptor asset = getAssets().openFd("resource_probe.txt");
             android.content.res.AssetFileDescriptor file = res.openRawResourceFd(raw)) {
            if (asset.getLength() != 20 || file.getLength() != 18) throw new AssertionError("Resource openFd mismatch");
        } catch (java.io.IOException error) { throw new AssertionError(error); }
        android.util.Log.i("JIAGU_COMPAT", "COMPAT_RESOURCES_PASSED pid=" + android.os.Process.myPid());
    }

    private String read(java.io.InputStream input) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[128]; int count;
        while ((count = input.read(data)) != -1) out.write(data, 0, count);
        return new String(out.toByteArray(), "UTF-8");
    }
}
