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

    private static Application sDelegateApplication;

    /**
     * jni层回调创建真实Application
     */
    public static void hook(Application application, String delegateApplicationName) {
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
            Class<?> applicationClass = classLoader.loadClass(delegateApplicationName);
            sDelegateApplication = (Application) applicationClass.newInstance();

            // 走DelegateApplication的生命周期
            Reflect.invokeMethod(Application.class, sDelegateApplication, "attach",
                    new Object[]{contextImpl}, Context.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

            // 替换ContextImpl的代理Application
            Reflect.invokeMethod(contextImpl.getClass(), contextImpl, "setOuterContext",
                    new Object[]{sDelegateApplication}, Context.class);

            // 替换ActivityThread的代理Application
            Object mMainThread = Reflect.getFieldValue(contextImpl.getClass(), contextImpl,
                    "mMainThread");
            Reflect.setFieldValue("android.app.ActivityThread", mMainThread, "mInitialApplication",
                    sDelegateApplication);
            // 替换ActivityThread的mAllApplications
            ArrayList<Application> mAllApplications =
                    (ArrayList<Application>) Reflect.getFieldValue("android.app.ActivityThread",
                            mMainThread, "mAllApplications");
            mAllApplications.add(sDelegateApplication);
            mAllApplications.remove(application);

            // 替换LoadedApk的代理Application
            Object loadedApk = Reflect.getFieldValue(contextImpl.getClass(), contextImpl,
                    "mPackageInfo");
            Reflect.setFieldValue("android.app.LoadedApk", loadedApk, "mApplication",
                    sDelegateApplication);

            // 替换LoadedApk中的mApplicationInfo中name
            ApplicationInfo applicationInfo =
                    (ApplicationInfo) Reflect.getFieldValue("android.app.LoadedApk",
                            loadedApk
                            , "mApplicationInfo");
            applicationInfo.className = sDelegateApplication.getClass().getName();


            sDelegateApplication.onCreate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
