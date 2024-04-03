package com.wpf.util.jiagulibrary.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import com.wpf.util.jiagulibrary.ApkConfig;
import dalvik.system.DexClassLoader;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Dex文件保护工具类
 *
 */
public class DexProtector {
    private static final String TAG = "StubApplication";

    public static final String decryptDexPath = "decryptDex";
    public static final String decryptODexPath = "decryptODex";
    public static final String decryptDexAssetsPath = "jiagu_wpf/";
    public static final String decryptDexExtension = "wpfjiagu";

    private Context context = null;
    private Class<?> loadedApkClass = null;
    private WeakReference<?> loadedApkRef = null;

    public DexProtector(Context ctx, String packageName) {
        this.context = ctx;
        try {
            // 初始化
            // 1. 得到当前的ActivityThread
            // 加载ActivityThread的字节码
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            // 利用反射得到currentActivityThread方法
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            // 调用currentActivityThread方法得到当前ActivityThread对象
            Object activityThread = currentActivityThreadMethod.invoke(null);

            // 2. 得到LoadedApk的弱引用
            //final ArrayMap<String, WeakReference<LoadedApk>> mPackages = new ArrayMap<String, WeakReference<LoadedApk>>();
            // 得到LoadedApk对象
            Field mPackagesField = activityThreadClass.getDeclaredField("mPackages");
            mPackagesField.setAccessible(true); //取消默认 Java 语言访问控制检查的能力（暴力反射）
            Map mPackages = (Map) mPackagesField.get(activityThread);
            // 得到LoadedApk对象的弱引用
            loadedApkRef = (WeakReference) mPackages.get(packageName);

            // 3. 修改LoadedApk对象中mClassLoader字段
            loadedApkClass = Class.forName("android.app.LoadedApk");
//			Field mClassLoaderField = loadedApkClass.getDeclaredField("mClassLoader");
//			mClassLoaderField.setAccessible(true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 加载 dex 文件集合
     * 这些 dex 文件已经解密
     * 参考博客 : https://hanshuliang.blog.csdn.net/article/details/109608605
     * <p>
     * 创建自己的 Element[] dexElements 数组
     * ( libcore/dalvik/src/main/java/dalvik/system/DexPathList.java )
     * 然后将 系统加载的 Element[] dexElements 数组 与 我们自己的 Element[] dexElements 数组进行合并操作
     */
    public static void loadDex(Context context, ArrayList<File> dexFiles, File optimizedDirectory)
            throws
            IllegalAccessException,
            InvocationTargetException,
            NoSuchMethodException,
            NoSuchFieldException {
        Log.e(TAG, "loadDex");
        /*
            需要执行的步骤
            1 . 获得系统 DexPathList 中的 Element[] dexElements 数组
                ( libcore/dalvik/src/main/java/dalvik/system/DexPathList.java )
            2 . 在本应用中创建 Element[] dexElements 数组 , 用于存放解密后的 dex 文件
            3 . 将 系统加载的 Element[] dexElements 数组
                与 我们自己的 Element[] dexElements 数组进行合并操作
            4 . 替换 ClassLoader 加载过程中的 Element[] dexElements 数组 ( 封装在 DexPathList 中 )
         */


        /*
            1 . 获得系统 DexPathList 中的 Element[] dexElements 数组

            第一阶段 : 在 Context 中调用 getClassLoader() 方法 , 可以拿到 PathClassLoader ;

            第二阶段 : 从 PathClassLoader 父类 BaseDexClassLoader 中找到 DexPathList ;

            第三阶段 : 获取封装在 DexPathList 类中的 Element[] dexElements 数组 ;

            上述的 DexPathList 对象 是 BaseDexClassLoader 的私有成员
            Element[] dexElements 数组 也是 DexPathList 的私有成员
            因此只能使用反射获取 Element[] dexElements 数组
         */

        // 阶段一二 : 调用 getClassLoader() 方法可以获取 PathClassLoader 对象
        // 从 PathClassLoader 对象中获取 private final DexPathList pathList 成员
        Field pathListField = ReflexUtils.reflexField(context.getClassLoader(), "pathList");
        // 获取 classLoader 对象对应的 DexPathList pathList 成员
        Object pathList = pathListField.get(context.getClassLoader());

        //阶段三 : 获取封装在 DexPathList 类中的 Element[] dexElements 数组
        Field dexElementsField = ReflexUtils.reflexField(pathList, "dexElements");
        // 获取 pathList 对象对应的 Element[] dexElements 数组成员
        Object[] dexElements = (Object[]) dexElementsField.get(pathList);



        /*
            2 . 在本应用中创建 Element[] dexElements 数组 , 用于存放解密后的 dex 文件
                不同的 Android 版本中 , 创建 Element[] dexElements 数组的方法不同 , 这里需要做兼容

         */
        Method makeDexElements;
        Object[] addElements = null;

        if (Build.VERSION.SDK_INT <=
                Build.VERSION_CODES.M) { // 5.0, 5.1  makeDexElements

            // 反射 5.0, 5.1, 6.0 版本的 DexPathList 中的 makeDexElements 方法
            makeDexElements = ReflexUtils.reflexMethod(
                    pathList, "makeDexElements",
                    ArrayList.class, File.class, ArrayList.class);
            ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
            addElements = (Object[]) makeDexElements.invoke(pathList, dexFiles,
                    optimizedDirectory,
                    suppressedExceptions);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // 7.0 以上版本 makePathElements

            // 反射 7.0 以上版本的 DexPathList 中的 makeDexElements 方法
            makeDexElements = ReflexUtils.reflexMethod(pathList, "makePathElements",
                    List.class, File.class, List.class);
            ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
            addElements = (Object[]) makeDexElements.invoke(pathList, dexFiles,
                    optimizedDirectory,
                    suppressedExceptions);

        }

        /*
            3 . 将 系统加载的 Element[] dexElements 数组
                与 我们自己的 Element[] dexElements 数组进行合并操作

            首先创建数组 , 数组类型与 dexElements 数组类型相同
            将 dexElements 数组中的元素拷贝到 newElements 前半部分, 拷贝元素个数是 dexElements.size
            将 addElements 数组中的元素拷贝到 newElements 后半部分, 拷贝元素个数是 dexElements.size
         */
        Object[] newElements = (Object[]) Array.newInstance(
                dexElements.getClass().getComponentType(),
                dexElements.length + addElements.length);

        // 将 dexElements 数组中的元素拷贝到 newElements 前半部分, 拷贝元素个数是 dexElements.size
        System.arraycopy(dexElements, 0, newElements, 0, dexElements.length);

        // 将 addElements 数组中的元素拷贝到 newElements 后半部分, 拷贝元素个数是 dexElements.size
        System.arraycopy(addElements, 0, newElements, dexElements.length, addElements.length);

        /*
            4 . 替换 ClassLoader 加载过程中的 Element[] dexElements 数组 ( 封装在 DexPathList 中 )
         */
        dexElementsField.set(pathList, newElements);

        Log.e(TAG, "loadDex 完成");

    }

    /**
     * 修改当前应用ClassLoader
     *
     * @param newClassLoader
     */
    private boolean setAppClassLoader(ClassLoader newClassLoader) {
        try {
            // 3. 修改LoadedApk对象中mClassLoader字段
            Field mClassLoaderField = loadedApkClass.getDeclaredField("mClassLoader");
            mClassLoaderField.setAccessible(true);
            mClassLoaderField.set(loadedApkRef.get(), newClassLoader); // 修改当前ClassLoader为自定义ClassLoader
            return true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 加载加密后的dex文件
     *
     */
    public void loadEncryptDex(ApkConfig apkConfig) {
        try {
            ArrayList<File> encryptDexPathList = getEncryptDex(context, apkConfig);
            // 经过优化的dex输出目录
            File odexDir = context.getDir(decryptODexPath, Context.MODE_PRIVATE);
            // 修改当前ClassLoader为自定义ClassLoader
            setAppClassLoader(getClassLoader(encryptDexPathList, ClassLoader.getSystemClassLoader(), odexDir));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private ClassLoader getClassLoader(ArrayList<File> encryptDexPathList, ClassLoader appClassLoader, File odexDir) {
        StringBuilder pathList = new StringBuilder();
        for (File encryptDexFile : encryptDexPathList) {
            pathList.append(encryptDexFile.getPath()).append(":");
        }

        // libs目录
        String libPath = context.getApplicationInfo().nativeLibraryDir;
        // 创建类加载器，加载解密后的dex文件
        ClassLoader dexClassLoader;
        if (appClassLoader != null) {
            dexClassLoader = new DexClassLoader(pathList.toString(), odexDir.getAbsolutePath(), libPath, appClassLoader);
        } else {
            dexClassLoader = new DexClassLoader(pathList.toString(), odexDir.getAbsolutePath(), libPath, context.getClassLoader());
        }
        return dexClassLoader;
    }


    public static ArrayList<File> getEncryptDex(Context context, ApkConfig apkConfig) throws Exception {
        Log.e(TAG, "开始解密");
        ArrayList<File> decryptDexFileList = new ArrayList<>();
        File dexPath = context.getDir(decryptDexPath, Context.MODE_PRIVATE);
        File[] dexList = dexPath.listFiles();
        if (dexPath.exists() && dexPath.isDirectory() && dexList != null && dexList.length > 0) {
            decryptDexFileList.addAll(Arrays.asList(dexList));
            Log.e(TAG, "已解密完成，返回缓存");
            return decryptDexFileList;
        }
        String[] allAssets = context.getAssets().list(decryptDexAssetsPath);
        ArrayList<String> encryptDexAssetsNameList = new ArrayList<>();
        for (String assets : allAssets) {
            if (assets.contains(decryptDexExtension)) {
                encryptDexAssetsNameList.add(assets);
            }
        }
        HashMap<String, InputStream> encryptDexNameMap = new HashMap<>();
        for (String dexName : encryptDexAssetsNameList) {
            encryptDexNameMap.put(dexName, context.getAssets().open(decryptDexAssetsPath + dexName));
        }

        for (ApkConfig.DexInfo configModel : apkConfig.dexInfoList) {
            String encryptDexName = configModel.dexName.replace(".dex", ".wpfjiagu");
            InputStream encryptDexIS = encryptDexNameMap.get(encryptDexName);
            File decryptDexFile = new File(dexPath, configModel.dexName);
            if (!decryptDexFile.exists()) {
                decryptDexFile.createNewFile();
            }
            IO.copyTo(encryptDexIS, new FileOutputStream(decryptDexFile));
            RandomAccessFile decryptDexAccessFile = new RandomAccessFile(decryptDexFile, "rw");
            if (configModel.dealList != null) {
                for (ApkConfig.DexInfo.DealInfo dealInfo : configModel.dealList) {
                    decryptDexAccessFile.seek(dealInfo.dealStartPos);
                    decryptDexAccessFile.write(dealInfo.srcBytes, 0, dealInfo.dealLength);
                }
            }
            encryptDexIS.close();
            decryptDexAccessFile.close();
            decryptDexFileList.add(decryptDexFile);
        }
        Log.e(TAG, "解密完成");
        return decryptDexFileList;
    }

    private void deleteDir(File dir) {
        for (File file : dir.listFiles()) {
            file.delete();
        }
        dir.delete();
    }
}
