package com.wpf.util.jiagulibrary;

import android.app.Application;
import android.content.Context;
import com.wpf.util.jiagulibrary.utils.ApplicationHook;
import com.wpf.util.jiagulibrary.utils.IO;
import com.wpf.util.jiagulibrary.utils.IntegrityVerifier;
import com.wpf.util.jiagulibrary.utils.StartupMetrics;

import static com.wpf.util.jiagulibrary.utils.AssetsUtil.copyJiaGu;

public class StubApp extends Application {
    /**
     * so的版本，格式为: v + 数字
     */
    public static final String VERSION = "v3_5";

    @Override
    protected void attachBaseContext(Context context) {
        StartupMetrics.start();
        try {
            super.attachBaseContext(context);
            long phase = System.nanoTime();
            IntegrityVerifier.verify(new java.io.File(context.getApplicationInfo().sourceDir));
            StartupMetrics.phase("integrity_ms", phase);
            phase = System.nanoTime();
            String library = copyJiaGu(context);
            StartupMetrics.phase("so_prepare_ms", phase);
            phase = System.nanoTime();
            System.load(library);
            StartupMetrics.phase("so_load_ms", phase);
            phase = System.nanoTime();
            attach(this);
            StartupMetrics.phase("native_attach_ms", phase);
            StartupMetrics.attached();
        } catch (RuntimeException | Error error) {
            StartupMetrics.failure("attach", error);
            throw error;
        }
    }

    @Override
    public void onCreate() {
        StartupMetrics.applicationStarted();
        long phase = System.nanoTime();
        try {
            super.onCreate();
            ApplicationHook.replaceApplicationContext(this);
            StartupMetrics.phase("application_context_and_oncreate_ms", phase);
            StartupMetrics.complete();
        } catch (RuntimeException | Error error) {
            StartupMetrics.failure("application", error);
            throw error;
        }
    }

    public byte[] invoke1(String s) {
        return IO.getDexData(s);
    }

    public void invoke2(Application application, String s) {
        ApplicationHook.hook(application, s);
    }

    public native static void attach(StubApp base);
}
