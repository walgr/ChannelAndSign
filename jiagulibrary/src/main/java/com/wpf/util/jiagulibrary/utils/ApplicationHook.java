package com.wpf.util.jiagulibrary.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentProvider;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.util.Log;
import com.wpf.util.jiagulibrary.StubApp;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class ApplicationHook {

    private static volatile Application sDelegateApplication;
    private static volatile ClassLoader sShellClassLoader;

    /** Framework component creation must use the loader containing business DEX. */
    public static ClassLoader componentClassLoader(ClassLoader frameworkLoader) {
        ClassLoader loader = sShellClassLoader;
        return loader != null ? loader : frameworkLoader;
    }

    /**
     * jni层回调创建真实Application
     */
    public static void hook(Application application, String delegateApplicationName) {
        sShellClassLoader = application.getClassLoader();
        if (TextUtils.isEmpty(delegateApplicationName) || StubApp.class.getName().equals(delegateApplicationName)) {
            sDelegateApplication = application;
            return;
        }

        Log.w("NDK_JIAGU", "hook");
        try {
            // 先获取到ContextImpl对象
            Context contextImpl = application.getBaseContext();
            // 创建插件中真实的Application且，执行生命周期
            ClassLoader classLoader = application.getClassLoader();
            Application delegate;
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                delegate = Api28.createApplication(application, classLoader, delegateApplicationName);
            } else {
                delegate = (Application) classLoader.loadClass(delegateApplicationName).newInstance();
            }

            // 走DelegateApplication的生命周期
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            attach.setAccessible(true);
            attach.invoke(delegate, contextImpl);
            // Plugin frameworks may replace LoadedApk.mClassLoader during attach.
            // Component creation must retain their dispatch loader (e.g. RePlugin).
            sShellClassLoader = application.getClassLoader();
            sDelegateApplication = delegate;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot attach original Application: " + delegateApplicationName, e);
        }
    }

    /** Kept separate so API 21-27 never resolve AppComponentFactory. */
    private static class Api28 {
        static Application createApplication(Application shell, ClassLoader loader, String name)
                throws Exception {
            String factoryName = shell.getApplicationInfo().appComponentFactory;
            if (TextUtils.isEmpty(factoryName)) {
                return (Application) loader.loadClass(name).newInstance();
            }
            // LoadedApk could not load the host factory before business DEX was
            // decrypted and fell back to the platform factory. Restore it before
            // Application.attach: hosts may register provider delegates there.
            if (factoryName.startsWith(".")) factoryName = shell.getPackageName() + factoryName;
            else if (factoryName.indexOf('.') < 0) factoryName = shell.getPackageName() + "." + factoryName;
            Context context = shell.getBaseContext();
            Object loadedApk = Reflect.getFieldValue(context.getClass(), context, "mPackageInfo");
            Object current = componentFactory(loadedApk, null);
            android.app.AppComponentFactory factory;
            if (current != null && current.getClass().getName().equals(factoryName)) {
                factory = (android.app.AppComponentFactory) current;
            } else {
                factory = (android.app.AppComponentFactory) loader.loadClass(factoryName).newInstance();
                componentFactory(loadedApk, factory);
            }
            return factory.instantiateApplication(loader, name);
        }
    }

    private static native Object componentFactory(Object loadedApk, Object replacement);

    /** Providers run before Application.onCreate and may cast the application context. */
    public static Context applicationContext(Application shell, Context fallback) {
        Application delegate = sDelegateApplication;
        if (delegate == null || delegate == shell) return fallback;
        try {
            Context context = shell.getBaseContext();
            Object loadedApk = Reflect.getFieldValue(context.getClass(), context, "mPackageInfo");
            // LoadedApk assigns the shell after attach returns. Publish only once
            // that assignment exists, so later provider/configuration contexts
            // resolve the same delegate without invoking onCreate prematurely.
            if (Reflect.getFieldValue("android.app.LoadedApk", loadedApk, "mApplication") == shell) {
                publishApplicationContext(shell, context, loadedApk);
            }
            return delegate;
        } catch (Exception error) {
            throw new IllegalStateException("Cannot expose original Application context", error);
        }
    }

    @SuppressWarnings("unchecked")
    private static void publishApplicationContext(Application shell, Context context, Object loadedApk) {
        migrateRegistrations(loadedApk, shell, sDelegateApplication);
        Reflect.invokeMethod(context.getClass(), context, "setOuterContext",
                new Object[]{sDelegateApplication}, Context.class);
        Object thread = Reflect.getFieldValue(context.getClass(), context, "mMainThread");
        Reflect.setFieldValue("android.app.ActivityThread", thread, "mInitialApplication", sDelegateApplication);
        ArrayList<Application> applications = (ArrayList<Application>) Reflect.getFieldValue(
                "android.app.ActivityThread", thread, "mAllApplications");
        if (!applications.contains(sDelegateApplication)) applications.add(sDelegateApplication);
        applications.remove(shell);
        Reflect.setFieldValue("android.app.LoadedApk", loadedApk, "mApplication", sDelegateApplication);
        ApplicationInfo info = (ApplicationInfo) Reflect.getFieldValue(
                "android.app.LoadedApk", loadedApk, "mApplicationInfo");
        info.className = sDelegateApplication.getClass().getName();
    }

    /**
     * hook 替换ApplicationContext
     *
     * @param application
     */
    public static void replaceApplicationContext(Application application) {
        if (sDelegateApplication == null || StubApp.class.getName().equals(sDelegateApplication.getClass().getName())) {
            return;
        }
        Log.w("NDK_JIAGU", "replaceApplicationContext");

        try {
            // 先获取到ContextImpl对象
            Context contextImpl = application.getBaseContext();

            // Providers/attach may already have registered callbacks with the shell
            // as ContextImpl's outer context. Keep their dispatcher identity valid
            // when the same base context starts returning the delegate instead.
            Object loadedApk = Reflect.getFieldValue(contextImpl.getClass(), contextImpl,
                    "mPackageInfo");
            publishApplicationContext(application, contextImpl, loadedApk);


            long started = System.nanoTime();
            try { sDelegateApplication.onCreate(); }
            finally { StartupMetrics.phase("delegate_oncreate_ms", started); }

        } catch (Exception e) {
            StartupMetrics.failure("delegate_oncreate", e);
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static void migrateRegistrations(Object loadedApk, Context previous, Context replacement) {
        Object[] maps = callbackMaps(loadedApk);
        for (int pair = 0; pair < 4; pair += 2) {
            Map<Object, Object> active = (Map<Object, Object>) maps[pair];
            synchronized (active) {
                for (int index = pair; index < pair + 2; index++) {
                    Map<Object, Object> contexts = (Map<Object, Object>) maps[index];
                    Map<Object, Object> callbacks = (Map<Object, Object>) contexts.get(previous);
                    if (callbacks == null) continue;
                    Map<Object, Object> target = (Map<Object, Object>) contexts.get(replacement);
                    if (target != null) {
                        for (Object key : callbacks.keySet()) {
                            if (target.containsKey(key)) throw new IllegalStateException("Duplicate early callback registration");
                        }
                    }
                    for (Object dispatcher : callbacks.values()) {
                        setDispatcherContext(dispatcher, replacement, pair == 0);
                    }
                    if (target == null) contexts.put(replacement, callbacks);
                    else target.putAll(callbacks);
                    contexts.remove(previous);
                }
            }
        }
    }

    private static native Object[] callbackMaps(Object loadedApk);
    private static native void setDispatcherContext(Object dispatcher, Context context, boolean service);

    /**
     * 修改已经存在ContentProvider中application
     *
     * @param application
     * @return
     */
    public static Application replaceContentProvider(Application application) {
        if (sDelegateApplication == null || StubApp.class.getName().equals(sDelegateApplication.getClass().getName())) {
            return application;
        }
        Log.w("NDK_JIAGU", "replaceContentProvider");
        try {
            // 替换LoadedApk的代理Application
            Context contextImpl = application.getBaseContext();
            Object loadedApk = Reflect.getFieldValue(contextImpl.getClass(), contextImpl,
                    "mPackageInfo");
            Reflect.setFieldValue("android.app.LoadedApk", loadedApk, "mApplication",
                    sDelegateApplication);

            Object activityThread = currentActivityThread();
            Map<Object, Object> mProviderMap =
                    (Map<Object, Object>) Reflect.getFieldValue(activityThread.getClass(),
                            activityThread, "mProviderMap");
            Set<Map.Entry<Object, Object>> entrySet = mProviderMap.entrySet();
            for (Map.Entry<Object, Object> entry : entrySet) {
                // 取出ContentProvider
                ContentProvider contentProvider =
                        (ContentProvider) Reflect.getFieldValue(entry.getValue().getClass(),
                                entry.getValue(), "mLocalProvider");

                if (contentProvider != null) {
                    // 修改ContentProvider中的context
                    Reflect.setFieldValue("android.content.ContentProvider", contentProvider,
                            "mContext", sDelegateApplication);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sDelegateApplication;
    }

    private static Object currentActivityThread() {
        try {
            @SuppressLint("PrivateApi") Class<?> cls = Class.forName("android.app.ActivityThread");
            @SuppressLint("PrivateApi") Method declaredMethod = cls.getDeclaredMethod("currentActivityThread");
            declaredMethod.setAccessible(true);
            return declaredMethod.invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
