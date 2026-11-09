package com.wpf.util.jiagulibrary;

import android.app.Application;
import android.content.Context;
import com.wpf.util.jiagulibrary.utils.ApplicationHook;
import com.wpf.util.jiagulibrary.utils.IO;

import static com.wpf.util.jiagulibrary.utils.AssetsUtil.copyJiaGu;

public class StubApp extends Application {
    /**
     * so的版本，格式为: v + 数字
     */
    public static final String VERSION = "v3_4";

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);

        System.load(copyJiaGu(context));

        attach(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ApplicationHook.replaceApplicationContext(this);
    }

    public byte[] invoke1(String s) {
        return IO.getDexData(s);
    }

    public void invoke2(Application application, String s) {
        ApplicationHook.hook(application, s);
    }

    public native static void attach(StubApp base);
}
