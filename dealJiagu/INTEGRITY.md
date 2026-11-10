# 兼容性与完整性校验

加固后默认生成 `assets/jiagu-integrity.bin` 和 `.sig`。记录包含最终 `classes.dex`（壳及加密业务载荷）、`AndroidManifest.xml`、`resources.arsc`、`res/`、`lib/<abi>/*.so` 与 `assets/` 全部文件（含业务 SO、壳 SO，排除完整性记录本身及目录）的 SHA-256，使用 RSA-2048/SHA256withRSA 签名。对应公钥注入壳 Java 代码并参与壳缓存键。`StubApp.attachBaseContext` 在加载 SO 或解密 DEX 之前验证签名及记录内容；缺失、重复、替换或损坏会明确失败。

完整性签名独立于 APK 部署证书，因此保留「加固 → 自行签名」及 XAPK 全部 split 重签流程。它不会绑定 APK 的 Android 签名证书，也不会拒绝仅更换部署证书的包。若要锁定发布证书，需要另外设置证书绑定策略，不能默认锁定加固前的第三方证书。

签名私钥保存在工具缓存 `cache/jiagu-integrity/private.pk8`，不写入 APK/XAPK、日志或壳源码。请将该缓存作为私有构建材料保管。删除此私钥后会生成新密钥并重建壳，已有 APK 仍使用其中原公钥正常验证。记录签名能防止只改内容或重算 SHA-256；它不能保证攻击者无法同时修改壳 DEX、公钥及验证代码。

当前保护范围是主 APK 的载荷、Manifest、资源表、res、assets（含壳 SO）与 lib 目录的业务 SO；split 内的代码、资源和 SO 未纳入壳的独立记录。SO 内容加密为可选项，依赖与预算控制及缓存限制见 [NATIVE_PROTECTION.md](NATIVE_PROTECTION.md)。资源路径混淆及轻量内容加密为可选项，加密载荷同时纳入签名，详见 [RESOURCES.md](RESOURCES.md)。XAPK 的各 APK 仍由统一部署证书签名与验签。

SO 提取缓存增加内容核对，损坏缓存会从已经验证的 APK 重新提取；新提取文件核对 SHA-256 和 ELF ABI，通过后才发布并加载。每个新进程均重新检查，验证开销与受保护文件总大小相关。

兼容性合同为 Android API 21+ 的 armeabi-v7a、arm64-v8a、x86、x86_64 ART，并依赖设备 ART DefineClass 符号/Hook 能力。启动会在加载壳 SO 前给出不支持 API/进程 ABI 的明确错误；这不代表所有 OEM/Android 版本都已实机验证。ARMv7 已接入 ARM/Thumb 类定义 Hook；各设备仍需执行启动回归。加固前同时拒绝重复加固、缺少主 classes.dex、超出载荷长度限制的 Application 名；Application 名按 UTF-8 字节写入载荷。

验证命令：

```text
gradlew :dealJiagu:verifyShellIntegrity :dealJiagu:verifyD2c :dealJiagu:verifyHybridPayload :dealJiagu:verifyAesPayload
gradlew :dealJiagu:verifyXapk -PxapkHarden
```

`verifyShellIntegrity` 在 JVM 上执行与 Android 壳相同的 Java 验证源码，测试修改/缺失记录、DEX/SO/Manifest、错误公钥、伪造签名和缓存内容损坏。兼容性矩阵测试验证准入逻辑，不等于对应设备启动测试。

2026-11-10：四 ABI 的新增窄整数 D2C/混合/VMP JNI 库编译通过；Android API 36 x86_64 ART 开启 CheckJNI 后，原 DEX、D2C、混合、全 VMP 的 17 个整数边界输入与全部 boolean 组合结果一致，日志 `build/protection-narrow-art-results.txt`。完整 XAPK 加固、统一签名及签名记录验证通过，日志 `build/protection-upgrade-final.log`。壳工程使用独立的短临时目录，避免 XAPK 嵌套路径触发 Windows CMake/Ninja 重生成失败；完成或失败后均清理该目录，不再删除输入旁的通用 `tmp` 目录。实际业务应用仍需单独执行启动、Provider、SDK、升级与多进程回归。

相同 Java 完整性源码已通过 Android API 36 x86_64 ART 的 RSA 内容验证及缓存 SO 损坏检测，日志 `build/protection-integrity-art-results.txt`；这项测试通过独立 dalvikvm 运行，未安装或启动业务应用。

## 壳构建的 Gradle 复用

加固代码根据壳 Wrapper 声明的精确版本，只读查找 `GRADLE_HOME` 及有效 Gradle User Home 下已完成安装的 `wrapper/dists` bin/all 分发。缓存目录优先采用 JVM 的 `gradle.user.home`，其次是 `GRADLE_USER_HOME`，最后是用户目录的 `.gradle`。找到同版本后直接调用该分发的 Gradle launcher，复用依赖缓存；不同下载镜像的 URL 哈希不会再引发同版本重复下载。不会改写 Wrapper 配置、复制分发包或调整用户的 Gradle 目录。

找不到可用分发时才回退到壳 Wrapper，并打印所需版本、缓存路径及可能下载的提示。JDK 同时用于启动进程的 `JAVA_HOME` 和 Gradle daemon 的 `org.gradle.java.home`。独立安装在任意目录且未通过 `GRADLE_HOME` 指定的 Gradle，无法自动发现；IDE 私有设置也不会自动传给独立加固程序。

`:dealJiagu:verifyShellGradle` 测试精确版本、bin/all、部分下载排除、空格路径及缓存配置优先级，并以不存在的镜像地址和 `--offline` 实测本地 Gradle 的版本查询与最小构建。此复用逻辑避免的是分发包重复下载，构建依赖缺失时仍可能需要下载依赖。
