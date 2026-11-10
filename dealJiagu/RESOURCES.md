# 资源加固

重新构建桌面工具或加固 JAR 后，资源完整性保护默认开启。桌面加固页提供「资源路径混淆」和「轻量资源内容加密」两个独立开关，默认关闭。命令行分别使用 `-obfuscateResourcePaths 1`、`-encryptResourceContents 1`；程序接口分别为 `JiaGu.deal(..., obfuscateResourcePaths = true, encryptResourceContents = true)`。

## 完整性保护

主 APK 的 `resources.arsc`、`res/` 文件及 `assets/` 文件与 DEX、Manifest 一并写入 RSA 签名的内容记录。每个进程在加载壳 SO、解密业务 DEX 前验证；修改、删除或新增受保护文件都会失败。记录本身与签名文件排除在自己的散列范围之外，目录条目不计入。包内不得使用保留的 `assets/jiagu-integrity.bin` 和 `.sig` 路径。

保护范围包含 assets 下的第三方数据和嵌入式 DEX；未开启内容加密时，其路径及文件内容保持原样。开启加密后，通过正常 AssetManager 读取的路径和内容仍保持原样，APK ZIP 内的选中文件会移入加密载荷。校验需要顺序读取所有受保护文件，较大的图片、音视频和离线数据会增加每次进程启动的 I/O；通过 `NDK_JIAGU_PERF` 的 `integrity_ms` 观察实际开销。记录最多支持 100000 个文件、32 MiB，超过时构建明确报错。

## 资源路径混淆

解析标准二进制资源表的全局字符串池，将其中引用的 `res/` 文件名换成随机等长名称，并同步修改 ZIP 路径。支持 UTF-8、UTF-16 字符串池，更新排序标志，保持全部资源 ID、资源名称、类型、限定符目录、扩展名及文件字节不变。文件名空间已满、非 ASCII 路径及没有资源表引用的文件保留原样；实际处理数量写入报告。既有 `getIdentifier`、布局引用及资源主题按原资源表工作。

路径混淆仅改文件名，不改 assets 的访问路径；单独开启时内容仍可直接读取。直接使用 `res/...` 硬编码路径、自读取 APK、检查原资源文件名的代码可能不兼容，因此路径混淆默认关闭，开启后应对实际业务做回归。签名后应使用最终 APK 安装，沿用原有 zipalign/签名流程。

## 轻量资源内容加密

按用户对启动速度的要求，默认单文件上限为 **256 KiB**、累计原始内容上限为 **8 MiB**。优先选择布局/XML，其后按大小选择业务图片、raw 和 assets。视频、音频及 zip/obb 归档直接跳过；大文件或超预算文件保持明文和完整性校验，不混入加密载荷。`resourceEncryption.encryptedFiles`、`encryptedBytes`、`skippedFiles` 与预算字段列出实际覆盖及跳过原因，不把全部资源计为已加密。

选中的内容压缩后以 64 KiB 块执行 AES-128-GCM 加密，随机 nonce、完整头和块序号参与认证。载荷为 `assets/jiagu-resources.bin`，它同时纳入 RSA 内容签名；每块认证及最终资源内容摘要全部通过后才发布缓存。分块处理避免把整个资源载荷放入内存。密钥按域隔离从配置密钥派生并注入壳，不记录到日志或保护报告。

系统在壳启动前就会读取资源表、图标和主题。构建时从 Manifest 的资源引用出发，沿资源表别名、主题/父主题/属性、二进制 XML 引用递归保留依赖，并覆盖全部配置变体。这些文件保留明文，详见 `bootstrapFiles` 和 `bootstrapReason`。缺少可解析的资源表、无法确定本地或 split 资源依赖时保守保留全部 res 文件；不盲目加密启动资源。当前明确拒绝不支持的 compact 资源项。`resources.arsc` 和其中的普通字符串值不加密。

运行时在原 Application 和 Provider attach 前处理资源。API 30+ 使用公开的 ResourcesLoader/ResourcesProvider，并由 Activity、配置上下文继承；API 21–29 用 LoadedApk 的框架入口重建资源，保留 split 与原包 assets 回退。解密文件与保留的 res 组成私有资源 APK，保持资源 ID、二进制 XML/PNG 文件字节，存储并对齐条目以支持 openFd。未加密的大 assets 直接从原 APK 读取，不复制进缓存；保留的 res 文件和资源表仍需参与镜像重建，这些 I/O 不受加密预算限制。

缓存位置是应用 `no_backup/jiagu-resources/`，以已签名载荷摘要区分版本，并用文件锁协调多进程。后续进程核对资源条目和内容摘要后直接复用；损坏缓存重新生成，失败暂存文件清理，发布后设为只读。启动日志增加 `resource_prepare_ms` 和 `resource_cache_hit`，前者包括缓存检查、首次解密/资源重建及挂载，不只代表 AES 耗时。校验仍有 I/O，不能把缓存复用称为零开销。

此方案保护分发 APK 中的选定文件内容；运行时存在私有明文资源包，拥有足够运行时访问能力仍可读取，壳中的密钥也不是不可提取的秘密。原生直接打开 base.apk、SDK 自行解析 ZIP、PackageManager 从原安装路径读业务文件等绕开应用资源对象的方式不保证兼容。升级后旧版本资源缓存可能保留至清理应用数据，不在其他进程可能使用时删除。开启内容加密后仍需对实际业务及 OEM 做验证。

XAPK 对主 APK 应用相同处理并在 `baseProtection.resources`、`baseProtection.resourceEncryption` 记录结果；资源 split、功能 split 和 OBB 保留原内容，全部 APK 按现有流程统一签名。它们不纳入主 APK 的资源内容签名、混淆或内容加密，不能把报告当成整个 XAPK 已做资源加密。

## 验证

`gradlew :dealJiagu:verifyResourceProtection` 检查两种字符串池、长字符串、碰撞、损坏边界、真实 aapt2 资源表解析、资源 ID/名称、ZIP 内容及对齐。`verifyResourceEncryption` 执行与 Android 相同的分块编解码和缓存实现，验证图片/布局/raw/assets 密文交付、启动依赖闭包、大小预算、错误密钥、头/块/标签篡改、缓存恢复与对齐。`verifyShellIntegrity` 验证资源修改、删除、新增拒绝及大清单。`verifyCompatibility` 共八项主机检查，包含完整 XAPK 内容加密和统一签名。

`verifyDeviceCompatibility -PdeviceSerial=<序列号>` 的独立双 DEX 示例同时开启路径混淆及内容加密，验证 Application/Activity/配置上下文的字符串、布局、真实 PNG 像素、Drawable、raw、assets/openFd 和未加密大 assets 回退。六个场景为首次启动、缓存复用、资源缓存损坏恢复、SO 缓存损坏恢复、远程进程和覆盖安装。报告与日志在 `build/compatibility/device-<序列号>/`。这些是独立示例验证，不等同于业务 APK 或全部厂商设备兼容性验收。

2026-11-10 验证：7 项主机检查（含完整 XAPK 加固、签名）通过，桌面模块编译通过；API 21 x86 和 API 36 x86_64 的独立双 DEX 资源示例各通过 5 个设备场景，测试包均已卸载。设备日志分别为 build/resource-device-api21.log、build/resource-device-api36.log；主机构建日志为 build/resource-full-verification.log。该轮未对 ARMv7/ARM64 或实际业务包重复执行资源设备回归。

2026-11-10 内容加密最终验证：八项主机检查、XAPK 主包内容加密/全成员统一签名及桌面布尔开关回归通过。API 21 x86、API 25 x86、API 36 x86_64 使用同一 APK 各通过六项设备场景并卸载测试包，包含加密布局/PNG、openFd、配置上下文、超过预算的大 assets 回退与资源缓存损坏恢复。

该独立小样例只加密 5 个文件、881 字节业务内容，保留 4 个系统启动文件，跳过 1 个 300000 字节 assets。资源准备阶段（包含缓存检查和挂载）首次/第二次分别为 API 21：23/7 ms，API 25：36/12 ms，API 36：75/23 ms。这些是单次示例数据，不代表实际业务 APK、8 MiB 预算上限或全部 OEM/ABI 的耗时和兼容性。ARMv7/ARM64、本轮未覆盖的 API 及真实 split 安装组合仍需设备回归。

构建日志：`build/resource-encryption-final-host.log`；设备日志：`build/resource-encryption-final-emulator-{5570,5574,5554}.log`。设备详细报告见各自 `build/compatibility/device-emulator-*/results.json`。