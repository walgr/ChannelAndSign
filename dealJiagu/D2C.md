# 混合保护：第三代方法抽离 + D2C + VMP

白名单中的受支持方法按复杂度分配：短直线计算编译为 C++/JNI（D2C）；含分支、循环或至少 24 条 DEX 指令的方法转换为自定义虚拟指令，由原生解释器执行（VMP）。两者均编译进现有 `libjiagu.so`，重建后的 DEX 只保留 native 声明。其余方法继续走第三代方法抽离与按类恢复。壳仍由原业务 ClassLoader 加载，使 ART 能解析该加载器里的 JNI 实现。

当前是受限整数计算编译器及 VMP，支持范围见下文。默认从 APK Manifest 主包名及其子包筛选可转换的方法，不匹配名称相似的其他包。不支持的方法会跳过并统计，继续使用第三代加固流程；开启详细日志可查看跳过原因。主包名与源码包名不一致时，以 Manifest 主包名为准。

VMP 使用自定义寄存器指令、独立程序计数器和分支目标；每次调用使用独立寄存器帧。指令记录按方法及字位置编码存入 SO，执行时逐条解码，不回填原 DEX 指令。此编码是混淆，不是以密钥保密为基础的加密。当前是基础 VMP，并非商业保护强度评估或保证。

## 使用

不需要手动配置即可启用主包名默认筛选及混合分配。若需覆盖默认选择，在待加固 `app.apk` 旁创建 `app.d2c.txt`，每行填写一个完整 DEX 方法描述符。例如：

```text
# 根据实际源码或反编译后的名称填写，不支持通配符
Lcom/example/security/Core;->mix(II)I
Lcom/example/security/Core;->checksum(I)I
```

随后照常使用桌面加固入口；命令行也可显式传入 `-d2cConfig "C:\path\rules.txt"`，其他加固参数照旧。Kotlin API 新增默认参数 `d2cConfigPath`，可指定配置路径。显式配置覆盖自动选择，为精确匹配，最多 256 个方法；配置为空、规则重复、方法不存在或含不支持的操作都会在写出加固 APK 前失败。自动筛选没有匹配方法时继续使用第三代加固，不影响打包。

当前支持非同步 static 方法，参数和返回值可使用 `int/boolean/byte/char/short` 的组合，包括：整数常量、移动、取反、加减乘除余数、位运算、移位、条件分支、跳转、循环、整数返回及 `int-to-byte/short/char` 转换。按实际类型生成 JNI 参数和返回值，byte/short 符号扩展、char 零扩展、boolean 返回归一化。支持方法重载、JNI 名称转义、多 DEX。使用无符号寄存器实现 Java 整数溢出；除零抛出 ArithmeticException，并处理 MIN_VALUE / -1。

暂不支持实例方法、long/float/double、对象/数组、调用其他方法、字段访问、switch、try/catch、同步方法。自动筛选会跳过这些方法；显式指定则报错。第一阶段建议只选择自有、纯整数计算方法。

新增的窄整数例子位于 `src/test/fixtures/d2c/NarrowSmoke.java`。`:dealJiagu:verifyD2c` 另外生成 `build/d2c/narrow-d2c`、`narrow-hybrid`、`narrow-vmp` 三种输出。对应的 Android CMake 测试工程位于 `src/test/fixtures/native-scalar`，通过 `GENERATED_DIR` 指定 `build/d2c` 的绝对路径。

生成的源码（含 VMP 程序和解释器）参与 SHA-256 缓存标识；实现或保护路径变化会生成新的原生壳缓存。日志分别输出 D2C、VMP 方法数量。APK 的包名、方法名称和签名发生变化后，应更新白名单。需要重新构建桌面工具或加固 JAR，旧版工具不会识别新配置。

## 验证

运行 `gradlew :dealJiagu:verifyD2c`，自动编译示例 Java、调用本机 Android SDK 的 D8，验证 native 声明无 code_item、保留未选方法、SHA1/Adler32、拒绝不支持的规则与方法。需要 SDK platform 33、build-tools 和 JDK 17。示例输出在根目录 `build/d2c/generated/`，测试入口在 `src/test/fixtures/d2c/D2cSmoke.java`。

运行 `gradlew :dealJiagu:verifyHybridPayload` 会自动编译示例 Application/Activity 与壳 Java 源码，再生成包含 D2C、VMP、抽离记录的加密载荷。此任务不依赖手工生成的 provider-shell 文件，也不负责安装或启动模拟器。输出位于 `build/hybrid-app/payload/`，其中 `protection.txt` 列出每个原生方法的保护路径。

2026-11-09 验证结果：

- 示例扩展为 11 个选定方法，回归任务验证 D2C/VMP 分配及 native 声明；同时验证含参数调试信息的 DEX，防止 native 方法被写入无效 code_item。
- x86、x86_64、arm64-v8a、armeabi-v7a 生成代码独立编译及与壳合并编译通过。这不扩大第三代 ART Hook 原有的 ABI 支持范围。
- Android 17/API 37 x86_64 ART，开启 CheckJNI，分别执行原 DEX、D2C、混合分配、全部 VMP 四个版本，100 组边界输入的结果一致，除零异常、重载、位运算、条件比较和循环通过。记录保存在 `build/d2c/hybrid-art-results.txt`。
- 默认主包名筛选已验证子包包含、相似包名排除、不支持方法跳过、显式选择优先及零匹配处理。
- 完整壳 APK 集成测试通过：示例包含 8 个 D2C 方法、4 个 VMP 方法，原 Application、Activity 和剩余 DEX 方法按类恢复；Android 17 x86_64 前台 Activity 输出 `HYBRID_APK_PASSED`。测试签名 APK 位于 `build/hybrid-app/hybrid-smoke-signed.apk`，启动日志位于 `build/hybrid-app/startup-results.txt`。这是独立示例应用，不是 Keep 加固包。
- 尚未用 Keep 真实业务方法验证混合保护，未生成新的 Keep 混合保护包。x86/arm64 原生执行也尚未实机验证。

2026-11-10 收尾验证：`:dealJiagu:verifyD2c`、`:dealJiagu:verifyHybridPayload`、`:dealJiagu:verifyAesPayload` 和 `:dealJiagu:processResources` 均通过。集成示例 APK 的签名及 16 KiB ZIP 对齐校验通过。
