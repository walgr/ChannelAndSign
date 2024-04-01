package com.wpf.util.jiadulibrary;

import java.util.List;

public class ApkConfig {
    public String srcApplicationName;
    public List<DexInfo> dexInfoList;

    public static class DexInfo {
        public String dexName;
        public String dexMd5;
        public List<DealInfo> dealList;

        public static class DealInfo {
            public long stepStartPos;
            public long stepEndPos;
            public long dealStartPos;
            public int dealLength;
            public byte[] srcBytes;
        }
    }
}


