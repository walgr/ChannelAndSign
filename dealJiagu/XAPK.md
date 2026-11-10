# XAPK 加固与签名

桌面「加固」和「签名打包」均支持选择、拖入 APK/XAPK，或从文件夹批量读取；扩展名不区分大小写。

- 加固：解析各 APK 的二进制 Manifest，识别唯一主 APK，对主包应用现有第三代方法抽离 + D2C/VMP 混合保护；输出 `原名_jiagu.xapk`。
- 签名：解包后对**所有 APK**进行对齐、重签和验签，包括原先已经签过名的 APK；确认所有成员证书一致，输出 `原名_sign.xapk`。
- 主 APK、split APK 的归档路径不变，保留 OBB、图标和其他资源；更新 `manifest.json` 已有的文件大小、MD5/SHA1/SHA256 和 `total_size` 字段，保留未知字段。
- 使用独立临时目录，成功后替换输出；失败保留源文件及已有输出，清理本次临时文件。拒绝重复路径、越界路径、多主包、重复 split、包名/版本不一致。

功能拆分 APK 中的代码目前保留原样，**不参与 D2C/VMP 或方法抽离**。这里不会把 split 合并成单 APK。仅加固的输出须再对整个 XAPK 统一签名；XAPK 外层 ZIP 本身不使用 Android 签名。

默认白名单来自主 APK 的 Manifest 包名及子包。规则文件使用外层 `原名.d2c.txt`，显式 `-d2cConfig` 优先。原 APK 没有自定义 Application 时使用 `android.app.Application`。

命令行继续使用 `-srcApk`，支持 XAPK；提供签名参数后直接输出 `原名_jiagu_sign.xapk`：

```text
java -jar 加固.jar -srcApk app.xapk -secretKey 0123456789abcdef -secretKeyVi fedcba9876543210 -sdkPath C:/Android/Sdk -signFilePath release.jks -signAlias release -keyStorePassword PASSWORD -keyPassword PASSWORD
```

也可调用 `PackageSigner.sign(..., inputApkPath = "app.xapk")` 单独签名。提供 SDK 或配置 `ANDROID_HOME`/`ANDROID_SDK_ROOT`/当前目录 `local.properties` 后优先使用 SDK 的 zipalign（16 KiB SO 对齐）；否则兼容使用项目自带 zipalign。

自动验证：`:dealJiagu:verifyXapk`，包含真实二进制 Manifest 的主包、配置拆分包、已有签名重签、证书一致、16 KiB 对齐、资源与元数据保留、失败回滚和畸形归档测试。测试生成的包不是完整业务 App，不能代替目标应用的设备启动验证。

追加 `-PxapkHarden` 会实际生成加固壳、加固主 APK、重签所有拆分包，并检查输出中的壳 Manifest、加密 DEX、Intel SO、split 内容及签名。需要完整 SDK/NDK 和壳工程构建依赖。
