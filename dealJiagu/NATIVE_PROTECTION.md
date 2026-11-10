# SO 完整性与轻量内容加密

主 APK 中 `lib/<abi>/*.so` 默认纳入 RSA 签名的 SHA-256 完整性记录。壳在加载任何壳 SO、原 Application 和 Provider 前检测替换、缺失、增加或损坏。APK 部署签名仍可在加固后自行完成。

## 开启

桌面勾选「轻量 SO 内容加密」，默认关闭；可填写英文逗号分隔的排除库名。CLI：

```text
-encryptNativeLibraries 1 -nativeExclusions libcustom_loader.so,libdirect_path.so
```

API：`JiaGu.deal(..., encryptNativeLibraries = true, nativeExclusions = setOf("libcustom_loader.so"))`。排除支持文件名、SONAME 或完整 ZIP 路径。排除只影响内容加密，完整性校验仍然开启。

## 实际覆盖

- 支持 ELF32/ELF64 小端的 armeabi-v7a、arm64-v8a、x86、x86_64，最低 API21。
- 分析每 ABI 的 `PT_DYNAMIC`、`DT_NEEDED`、`DT_SONAME`。只加密没有被其他本包 SO 静态引用的入口库，依赖库和依赖环保留明文，由系统 linker 解析。不能识别的 ELF 会让该 ABI 全部保留明文。
- AES-128-GCM，每块 64 KiB，每次随机 nonce；密钥与资源加密采用不同派生域。单库最多 4 MiB，每 ABI 最多 16 MiB，优先小库，预算以外保留明文。
- 加密库从原 `lib/` 路径移至 `assets/jiagu-native/<abi>/<库名>.bin`，避免安装器将密文当作 ELF。每个受影响 ABI 添加一个有效的 `libjiagu_abi_marker.so`（复用该 ABI 的壳二进制，不包含业务原文，也不加载执行），维持系统 ABI 选择。标记文件会增加包体。
- XAPK 只加密主 APK 的符合条件 SO，split 保持原内容并统一签名。扫描 split 的 `DT_NEEDED`，主包被引用库保留明文；split ELF 无法分析时，保守保留主包全部 SO。split 内容本身未纳入壳的独立完整性记录。

## 运行时与性能

完整性验证后、原 Application 的 `attachBaseContext` 前，只解密安装 ABI 的载荷到 `no_backup/jiagu-native/<签名载荷版本>/<abi>/`。64 KiB 分块处理、进程锁、原文 SHA-256 核对、临时文件原子发布、只读缓存；缓存内容损坏会重建。同一版本的后续进程只核对摘要并复用，不重复解密。

既有 ClassLoader 的 native 搜索路径前置缓存，保留原业务 DEX/ClassLoader 和系统依赖路径；不提前调用业务 `System.loadLibrary`，不改变 `JNI_OnLoad` 时机。API21/22 更新 File[] 路径；API23+ 使用平台自身构造的 native path element。业务 SO 仍由系统 linker 正常加载。

`NDK_JIAGU_PERF.phasesMs.native_prepare_ms` 记录业务 SO 缓存核对/解密/路径安装时间，`native_cache_hit` 是 0/1 状态。它们与壳自身的 `so_prepare_ms`、`so_cache_hit` 不同。

这是磁盘载荷保护：运行时私有目录和进程内存在原文；密钥位于壳中，不宣称能抵抗可调试、root 或修改壳的攻击者。缓存按版本隔离，旧版本不会在其他进程可能仍映射时主动删除。

## 兼容性范围

常规 `System.loadLibrary` 与 ELF 静态依赖是本实现的覆盖范围。直接 `System.load(安装目录绝对路径)`、原生代码自行 `dlopen`（包括按名称动态加载另一个加密入口库）、ReLinker、自行解压 APK、自行创建 ClassLoader，以及业务修改 native 搜索路径的 SDK，需要排除相关入口库或单独适配。Java 搜索路径修改不等于改写所有原生 linker 的搜索路径，第三方 SDK 不会自动改写调用点。Native bridge 与厂商私有 ClassLoader 行为需另行实测。

报告 `nativeProtection` 给出实际加密列表、字节数、每个保留明文库的原因和预算。XAPK 的数据位于 `baseProtection.nativeProtection`。开启开关不代表所有 SO 都被加密。

## 验证

`:dealJiagu:verifyNativeProtection` 使用 NDK 编译四 ABI 的真实 JNI 入口、静态依赖和独立库，验证依赖分析、排除、保守回退、密文认证/截断、错误密钥、缓存复用/损坏恢复。完整性回归包含业务 SO 的篡改、缺失和新增拒绝。

`:dealJiagu:verifyDeviceCompatibility -PdeviceSerial=<序列号>` 使用独立测试包，在原 Application.attachBaseContext 中加载加密 JNI，验证 `DT_NEEDED`、`JNI_OnLoad` 的 FindClass/动态注册、普通 JNI 导出和多进程。测试 Manifest 使用 `extractNativeLibs=false`，验证 APK 直接加载保留的明文依赖。包含首次安装、缓存复用、业务 SO/资源/壳 SO 缓存损坏恢复、远程 Provider 和同证书升级七个场景，结束卸载测试包。

测试结果只证明独立示例与指定设备，不能替代业务 APK 的 SDK、ABI、系统和 OEM 回归。

2026-11-10 实测：四 ABI JNI 夹具构建与主机加密回归通过；API21 x86、API25 x86、API36 x86_64 各七场景通过。API21/API36 另各运行七场景的「主包全部业务 SO 加密，仅剩 ABI 标记库」模式，均通过。所有测试安装均已卸载。ARMv7/ARM64 本轮完成编译、ELF/加密/缓存主机验证，未执行设备 JNI 启动验证。

全入口加密模式可在上述设备命令中添加 `-PnativeRootsOnly`；结果保存到 `build/compatibility/device-<序列号>-native-roots/`，与普通依赖场景分开。
