# 兼容性与完整性校验

加固后默认生成 `assets/jiagu-integrity.bin` 和 `.sig`。记录包含最终 `classes.dex`（壳及加密业务载荷）、`AndroidManifest.xml` 与包内全部壳 SO asset 的 SHA-256，使用 RSA-2048/SHA256withRSA 签名。对应公钥注入壳 Java 代码并参与壳缓存键。`StubApp.attachBaseContext` 在加载 SO 或解密 DEX 之前验证签名及记录内容；缺失、重复、替换或损坏会明确失败。

完整性签名独立于 APK 部署证书，因此保留「加固 → 自行签名」及 XAPK 全部 split 重签流程。它不会绑定 APK 的 Android 签名证书，也不会拒绝仅更换部署证书的包。若要锁定发布证书，需要另外设置证书绑定策略，不能默认锁定加固前的第三方证书。

签名私钥保存在工具缓存 `cache/jiagu-integrity/private.pk8`，不写入 APK/XAPK、日志或壳源码。请将该缓存作为私有构建材料保管。删除此私钥后会生成新密钥并重建壳，已有 APK 仍使用其中原公钥正常验证。记录签名能防止只改内容或重算 SHA-256；它不能保证攻击者无法同时修改壳 DEX、公钥及验证代码。

当前保护范围是主 APK 的载荷、Manifest 和壳 SO；未纳入普通资源、第三方 SO 以及功能 split 内的代码。XAPK 的各 APK 仍由统一部署证书签名与验签。

SO 提取缓存增加内容核对，损坏缓存会从已经验证的 APK 重新提取；新提取文件核对 SHA-256 和 ELF ABI，通过后才发布并加载。每个新进程均重新检查，验证开销与受保护文件总大小相关。

兼容性合同仍为 Android API 26+ 的 arm64-v8a、x86、x86_64 ART，并依赖设备 ART DefineClass 符号/Hook 能力。启动会在加载壳 SO 前给出不支持 API/进程 ABI 的明确错误；这不代表所有 OEM/Android 版本都已实机验证。ARMv7 代码可编译，不代表支持第三代方法恢复。加固前同时拒绝重复加固、缺少主 classes.dex、超出载荷长度限制的 Application 名；Application 名按 UTF-8 字节写入载荷。

验证命令：

```text
gradlew :dealJiagu:verifyShellIntegrity :dealJiagu:verifyD2c :dealJiagu:verifyHybridPayload :dealJiagu:verifyAesPayload
gradlew :dealJiagu:verifyXapk -PxapkHarden
```

`verifyShellIntegrity` 在 JVM 上执行与 Android 壳相同的 Java 验证源码，测试修改/缺失记录、DEX/SO/Manifest、错误公钥、伪造签名和缓存内容损坏。兼容性矩阵测试验证准入逻辑，不等于对应设备启动测试。

2026-11-10：四 ABI 的新增窄整数 D2C/混合/VMP JNI 库编译通过；Android API 36 x86_64 ART 开启 CheckJNI 后，原 DEX、D2C、混合、全 VMP 的 17 个整数边界输入与全部 boolean 组合结果一致，日志 `build/protection-narrow-art-results.txt`。完整 XAPK 加固、统一签名及签名记录验证通过，日志 `build/protection-upgrade-final.log`。壳工程使用独立的短临时目录，避免 XAPK 嵌套路径触发 Windows CMake/Ninja 重生成失败；完成或失败后均清理该目录，不再删除输入旁的通用 `tmp` 目录。实际业务应用仍需单独执行启动、Provider、SDK、升级与多进程回归。

相同 Java 完整性源码已通过 Android API 36 x86_64 ART 的 RSA 内容验证及缓存 SO 损坏检测，日志 `build/protection-integrity-art-results.txt`；这项测试通过独立 dalvikvm 运行，未安装或启动业务应用。
