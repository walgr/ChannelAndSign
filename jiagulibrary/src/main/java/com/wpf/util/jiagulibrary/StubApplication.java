package com.wpf.util.jiagulibrary;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.ArrayMap;
import com.wpf.util.jiagulibrary.utils.IO;
import com.wpf.util.jiagulibrary.utils.Reflect;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import static com.wpf.util.jiagulibrary.utils.DexProtector.*;

public class StubApplication extends Application {
    private static final String TAG = "StubApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        config = getConfig();
        srcAppClassName = config.srcApplicationName;
        loadDexClassLoader();
    }

    private ApkConfig config;

    private ApkConfig getConfig() {
        try (InputStream configIS = getAssets().open("jiagu.config")) {
            String configStr = new String(IO.getInputStreamData(configIS));
            JSONObject jsonObject = new JSONObject(configStr);
            String srcApplicationName = jsonObject.optString("srcApplicationName");
            JSONArray dexInfoListJson = jsonObject.getJSONArray("dexInfoList");
            ArrayList<ApkConfig.DexInfo> dexInfoList = new ArrayList<>();
            for (int i = 0; i < dexInfoListJson.length(); ++i) {
                JSONObject dexInfoJson = dexInfoListJson.getJSONObject(i);
                ApkConfig.DexInfo dexInfo = new ApkConfig.DexInfo();
                dexInfo.dexName = dexInfoJson.optString("dexName");
                dexInfo.dexMd5 = dexInfoJson.optString("dexMd5");
                dexInfo.dealList = new ArrayList<>();
                JSONArray dealInfoListJson = dexInfoJson.getJSONArray("dealList");
                for (int j = 0; j < dealInfoListJson.length(); ++j) {
                    JSONObject dealInfoJson = dealInfoListJson.getJSONObject(j);
                    ApkConfig.DexInfo.DealInfo dealInfo = new ApkConfig.DexInfo.DealInfo();
                    dealInfo.stepStartPos = dealInfoJson.optLong("stepStartPos");
                    dealInfo.stepEndPos = dealInfoJson.optLong("stepEndPos");
                    dealInfo.dealStartPos = dealInfoJson.optLong("dealStartPos");
                    dealInfo.dealLength = dealInfoJson.optInt("dealLength");
                    JSONArray bytesJson = dealInfoJson.getJSONArray("srcBytes");
                    byte[] srcBytes = new byte[bytesJson.length()];
                    for (int k = 0; k < srcBytes.length; k++) {
                        srcBytes[k] = (byte) bytesJson.getInt(k);
                    }
                    dealInfo.srcBytes = srcBytes;
                    dexInfo.dealList.add(dealInfo);
                }
                dexInfoList.add(dexInfo);
            }
            ApkConfig apkConfig = new ApkConfig();
            apkConfig.srcApplicationName = srcApplicationName;
            apkConfig.dexInfoList = dexInfoList;
            return apkConfig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String srcAppClassName = "";

    private void loadDexClassLoader() {
        try {
            ArrayList<File> encryptDexPathList = getEncryptDex(this, config);
            // 经过优化的dex输出目录
            File odexDir = getDir(decryptODexPath, Context.MODE_PRIVATE);
            loadDex(this, encryptDexPathList, odexDir);
//            new DexProtector(this, super.getPackageName()).loadEncryptDex(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            delegate = changeTopApplication(srcAppClassName);
            delegate.onCreate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 调用 applicationExchange 替换 Application
     * 该成员就是替换后的 Application
     */
    private Application delegate;

    // 修改应用上下文
    // http://blog.csdn.net/jltxgcy/article/details/50540309
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private Application changeTopApplication(String appClassName) {
        //有值的话调用该Applicaiton
        Object currentActivityThread = Reflect.invokeStaticMethod(
                "android.app.ActivityThread", "currentActivityThread",
                new Class[]{}, new Object[]{});
        Object mBoundApplication = Reflect.getFieldValue(
                "android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");
        Object loadedApkInfo = Reflect.getFieldValue(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication, "info");
        //把当前进程的mApplication 设置成了null
        Reflect.setFieldValue("android.app.LoadedApk", loadedApkInfo, "mApplication", null);
        Object oldApplication = Reflect.getFieldValue(
                "android.app.ActivityThread", currentActivityThread,
                "mInitialApplication");
        //http://www.codeceo.com/article/android-context.html
        ArrayList<Application> mAllApplications = (ArrayList<Application>) Reflect
                .getFieldValue("android.app.ActivityThread",
                        currentActivityThread, "mAllApplications");
        mAllApplications.remove(oldApplication);//删除oldApplication

        ApplicationInfo loadedApk = (ApplicationInfo) Reflect
                .getFieldValue("android.app.LoadedApk", loadedApkInfo,
                        "mApplicationInfo");
        ApplicationInfo appBindData = (ApplicationInfo) Reflect
                .getFieldValue("android.app.ActivityThread$AppBindData",
                        mBoundApplication, "appInfo");

        loadedApk.className = appClassName;
        appBindData.className = appClassName;

        delegate = (Application) Reflect.invokeMethod(
                "android.app.LoadedApk", loadedApkInfo, "makeApplication",
                new Object[]{false, null},
                boolean.class, Instrumentation.class);//执行 makeApplication（false,null）
        Reflect.setFieldOjbect("android.app.ActivityThread",
                "mInitialApplication", currentActivityThread, delegate);
//
        //由于源码ActivityThread中handleBindApplication方法绑定Application后会调用installContentProviders，
        //此时传入的context仍为壳Application，故此处进手动安装ContentProviders，调用完成后，清空原providers
        clearContentProviders(delegate, currentActivityThread, mBoundApplication);

        ArrayMap mProviderMap = (ArrayMap) Reflect.getFieldOjbect(
                "android.app.ActivityThread", currentActivityThread,
                "mProviderMap");
        Iterator it = mProviderMap.values().iterator();
        while (it.hasNext()) {
            Object providerClientRecord = it.next();
            Object localProvider = Reflect.getFieldOjbect(
                    "android.app.ActivityThread$ProviderClientRecord",
                    providerClientRecord, "mLocalProvider");
            Reflect.setFieldOjbect("android.content.ContentProvider",
                    "mContext", localProvider, delegate);
        }
        return delegate;
    }

    private void clearContentProviders(Application app, Object currentActivityThread, Object boundApplication) {
        if (app == null) return;
        Reflect.setFieldValue(boundApplication.getClass().getName(),
                boundApplication, "providers", null);
//        List providers = (List) Reflect.getFieldOjbect(boundApplication.getClass().getName(),
//                boundApplication, "providers");
//        Log.e(TAG, "providers：" + providers);
//        if (providers != null) {
//            Reflect.invokeMethod("android.app.ActivityThread", "installContentProviders", currentActivityThread, new Class[]{Context.class, List.class}, new Object[]{app, providers});
//            providers.clear();
//        }
    }
}
