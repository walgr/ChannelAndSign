package com.wpf.util.jiagulibrary;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.util.Log;
import com.wpf.util.jiagulibrary.utils.IO;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static com.wpf.util.jiagulibrary.utils.DexProtector.*;

public class Stub2Application extends Application {
    private static final String TAG = "StubApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        config = getConfig();
        srcAppClassName = config.srcApplicationName;
        loadDexClassLoader(this);
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

    private void loadDexClassLoader(Context context) {
        try {
            ArrayList<File> encryptDexPathList = getEncryptDex(this, config);
            // 经过优化的dex输出目录
            File odexDir = getDir(decryptODexPath, Context.MODE_PRIVATE);
            loadDex(context, encryptDexPathList, odexDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            delegate = bindRealApplication(delegate, srcAppClassName);
            delegate.onCreate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        if (!TextUtils.isEmpty(srcAppClassName)) {
//            Application app = changeTopApplication(srcAppClassName);
//            if (app != null) {
//                app.onCreate();
//            } else {
//                Log.e(TAG, "changeTopApplication failure!!!");
//            }
//        }
    }

    /**
     * 作用于修改ContentProvider的context
     * 让代码走入if中的第三段中
     */
    @Override
    public String getPackageName() {
        if (!TextUtils.isEmpty(srcAppClassName)) {
            return "com.wpf.application";
        }
        return super.getPackageName();
    }

    @Override
    public Context createPackageContext(String packageName, int flags) {
        try {
            if (TextUtils.isEmpty(srcAppClassName)) {
                return super.createPackageContext(packageName, flags);
            }
            delegate = bindRealApplication(delegate, srcAppClassName);
            return delegate;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 调用 applicationExchange 替换 Application
     * 该成员就是替换后的 Application
     */
    private Application delegate;

    private synchronized Application bindRealApplication(Application delegate, String srcAppClassName) throws Exception {
        // 先判断是否有配置 Application ,
        // 那么在 Manifest.xml 中的 meta-data 元数据 app_name 不为空
        // 如果开发者没有自定义 Application , 没有配置元数据 , 直接退出
        if (TextUtils.isEmpty(srcAppClassName)) {
            return null;
        }

        // 获取上下文对象 , 保存下来 , 之后要使用
        Context baseContext = getBaseContext();

        // 通过反射获取 Application , 系统也是进行的反射操作
        if (delegate == null) {
            Class<?> delegateClass = Class.forName(srcAppClassName);

            // 创建用户真实配置的 Application
            delegate = (Application) delegateClass.newInstance();
            // 调用 Application 的 attach 函数
            // 该函数无法直接调用 , 也需要通过反射调用
            // 这里先通过反射获取 Application 的 attach 函数
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            // attach 方法是私有的 , 设置 attach 方法允许访问
            attach.setAccessible(true);

            // 获取上下文对象 ,
            // 该 Context 是通过调用 Application 的 attachBaseContext 方法传入的 ContextImpl
            // 将该上下文对象传入 Application 的 attach 方法中
            attach.invoke(delegate, baseContext);
        }


            /*
                参考 : https://hanshuliang.blog.csdn.net/article/details/111569017 博客
                查询应该替换哪些对象中的哪些成员

                截止到此处, Application 创建完毕 , 下面开始逐个替换下面的 Application

                ① ContextImpl 的 private Context mOuterContext
                    成员是 kim.hsl.multipledex.ProxyApplication 对象 ;

                ② ActivityThread 中的 ArrayList<Application> mAllApplications
                    集合中添加了 kim.hsl.multipledex.ProxyApplication 对象 ;

                ③ LoadedApk 中的 mApplication 成员是 kim.hsl.multipledex.ProxyApplication 对象 ;

                ④ ActivityThread 中的 Application mInitialApplication
                    成员是 kim.hsl.multipledex.ProxyApplication 对象 ;
             */

        // I . 替换 ① ContextImpl 的 private Context mOuterContext
        //  成员是 kim.hsl.multipledex.ProxyApplication 对象
        Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
        // 获取 ContextImpl 中的 mOuterContext 成员
        Field mOuterContextField = contextImplClass.getDeclaredField("mOuterContext");
        // mOuterContext 成员是私有的 , 设置可访问性
        mOuterContextField.setAccessible(true);
        // ContextImpl 就是应用的 Context , 直接通过 getBaseContext() 获取即可
        mOuterContextField.set(baseContext, delegate);


        // II . 替换 ④ ActivityThread 中的 Application mInitialApplication
        //                    成员是 kim.hsl.multipledex.ProxyApplication 对象 ;
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        // 获取 ActivityThread 中的 mInitialApplication 成员
        Field mInitialApplicationField =
                activityThreadClass.getDeclaredField("mInitialApplication");
        // mInitialApplication 成员是私有的 , 设置可访问性
        mInitialApplicationField.setAccessible(true);

        // 从 ContextImpl 对象中获取其 ActivityThread mMainThread 成员变量
        Field mMainThreadField = contextImplClass.getDeclaredField("mMainThread");
        mMainThreadField.setAccessible(true);
        // ContextImpl 就是本应用的上下文对象 , 调用 getBaseContext 方法获得
        Object mMainThread = mMainThreadField.get(baseContext);

        // ContextImpl 就是应用的 Context , 直接通过 getBaseContext() 获取即可
        mInitialApplicationField.set(mMainThread, delegate);


        // III . 替换 ② ActivityThread 中的 ArrayList<Application> mAllApplications
        //                    集合中添加了 kim.hsl.multipledex.ProxyApplication 对象 ;

        // 获取 ActivityThread 中的 mAllApplications 成员
        Field mAllApplicationsField =
                activityThreadClass.getDeclaredField("mAllApplications");
        // mAllApplications 成员是私有的 , 设置可访问性
        mAllApplicationsField.setAccessible(true);

        // 获取 ActivityThread 中的 ArrayList<Application> mAllApplications 队列
        ArrayList<Application> mAllApplications =
                (ArrayList<Application>) mAllApplicationsField.get(mMainThread);
        // 将真实的 Application 添加到上述队列中
        mAllApplications.add(delegate);


        // IV . 替换 ③ LoadedApk 中的 mApplication
        //          成员是 kim.hsl.multipledex.ProxyApplication 对象

        // 1. 先获取 LoadedApk 对象
        // LoadedApk 是 ContextImpl 中的 LoadedApk mPackageInfo 成员变量
        // 从 ContextImpl 对象中获取其 LoadedApk mPackageInfo 成员变量
        Field mPackageInfoField = contextImplClass.getDeclaredField("mPackageInfo");
        mPackageInfoField.setAccessible(true);
        // ContextImpl 就是本应用的上下文对象 , 调用 getBaseContext 方法获得
        Object mPackageInfo = mPackageInfoField.get(baseContext);

        // 2. 获取 LoadedApk 对象中的 mApplication 成员
        Class<?> loadedApkClass = Class.forName("android.app.LoadedApk");
        // 获取 ActivityThread 中的 mInitialApplication 成员
        Field mApplicationField =
                loadedApkClass.getDeclaredField("mApplication");
        // LoadedApk 中的 mApplication 成员是私有的 , 设置可访问性
        mApplicationField.setAccessible(true);

        // 3. 将 Application 设置给 LoadedApk 中的 mApplication 成员
        mApplicationField.set(mPackageInfo, delegate);


        // V . 下一步操作替换替换 ApplicationInfo 中的 className , 该操作不是必须的 , 不替换也不会报错
        // 在应用中可能需要操作获取应用的相关信息 , 如果希望获取准确的信息 , 需要替换 ApplicationInfo
        // ApplicationInfo 在 LoadedApk 中

        Field mApplicationInfoField = loadedApkClass.getDeclaredField("mApplicationInfo");
        // 设置该字段可访问
        mApplicationInfoField.setAccessible(true);

        // mPackageInfo 就是 LoadedApk 对象
        // mApplicationInfo 就是从 LoadedApk 对象中获得的 mApplicationInfo 字段
        ApplicationInfo mApplicationInfo = (ApplicationInfo) mApplicationInfoField.get(mPackageInfo);

        // 设置 ApplicationInfo 中的 className 字段值
        mApplicationInfo.className = srcAppClassName;


        // 再次调用 onCreate 方法
//        delegate.onCreate();
        return delegate;
    }
}
