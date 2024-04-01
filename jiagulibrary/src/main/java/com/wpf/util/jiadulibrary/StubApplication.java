package com.wpf.util.jiadulibrary;

import android.app.Application;
import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StubApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        dealEncryptDex();
    }

    private List<File> dealEncryptDex() {
        try (InputStream configIS = getAssets().open("jiagu.config")) {
            String configStr = new String(getInputStreamData(configIS));
            ApkConfig configParams = new Gson().fromJson(configStr, new TypeToken<ApkConfig>() {}.getType());
            String[] allAssets = getAssets().list("");
            ArrayList<String> encryptDexAssetsNameList = new ArrayList<>();
            for (String assets : allAssets) {
                if (assets.contains("wpfjiagu")) {
                    encryptDexAssetsNameList.add(assets);
                }
            }
            HashMap<String, InputStream> encryptDexNameMap = new HashMap<>();
            for (String dexName : encryptDexAssetsNameList) {
                encryptDexNameMap.put(dexName, getAssets().open(dexName));
            }
            List<File> decryptDexFileList = new ArrayList<>();
            for (ApkConfig.DexInfo configModel : configParams.dexInfoList) {
                String encryptDexName = configModel.dexName.replace(".dex", ".wpfjiagu");
                InputStream encryptDexIS = encryptDexNameMap.get(encryptDexName);
                File decryptDexFile = new File(getCacheDir().getPath() + File.separator + configModel.dexName);
                if (!decryptDexFile.exists()) {
                    decryptDexFile.createNewFile();
                }
                copyTo(encryptDexIS, new FileOutputStream(decryptDexFile));
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
            return decryptDexFileList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getInputStreamData(InputStream inputStream) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(Math.max(8 * 1024, inputStream.available()));
            copyTo(inputStream, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long copyTo(InputStream inputStream, OutputStream out) {
        try {
            long bytesCopied = 0;
            byte[] buffer = new byte[8 * 1024];
            int bytes = inputStream.read(buffer);
            while (bytes >= 0) {
                out.write(buffer, 0, bytes);
                bytesCopied += bytes;
                bytes = inputStream.read(buffer);
            }
            return bytesCopied;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
