# 第三代方法抽离 + D2C（第一阶段）

关键方法可通过精确白名单转换成 C++/JNI，编译进现有 `libjiagu.so`。重建后的 DEX 只保留这些方法的 native 声明，不再携带其实现；其他方法继续走现有第三代方法抽离与按类恢复。壳仍由原业务 ClassLoader 加载，使 ART 能解析该加载器里的 JNI 实现。

这是受限的 D2C 编译器，尚未实现 VMP。默认从 APK Manifest 主包名及其子包筛选可转换的方法，不匹配名称相似的其他包。不支持的方法会跳过并统计，继续使用第三代加固流程；开启详细日志可查看跳过原因。主包名与源码包名不一致时，以 Manifest 主包名为准。

## 使用

不需要手动配置即可启用主包名默认筛选。若需覆盖默认选择，在待加固 `app.apk` 旁创建 `app.d2c.txt`，每行填写一个完整 DEX 方法描述符。例如：

```text
# 根据实际源码或反编译后的名称填写，不支持通配符
Lcom/example/security/Core;->mix(II)I
Lcom/example/security/Core;->checksum(I)I
```

随后照常使用桌面加固入口；命令行也可显式传入 `-d2cConfig "C:\path\rules.txt"`，其他加固参数照旧。Kotlin API 新增默认参数 `d2cConfigPath`，可指定配置路径。显式配置覆盖自动选择，为精确匹配，最多 256 个方法；配置为空、规则重复、方法不存在或含不支持的操作都会在写出加固 APK 前失败。自动筛选没有匹配方法时继续使用第三代加固，不影响打包。

当前支持非同步的 `static int method(int...)`，包括：整数常量、移动、取反、加减乘除余数、位运算、移位、条件分支、跳转、循环和整数返回。支持方法重载、JNI 名称转义、多 DEX。使用无符号寄存器实现 Java 整数溢出；除零抛出 ArithmeticException，并处理 MIN_VALUE / -1。

暂不支持实例方法、long/float/double、对象/数组、调用其他方法、字段访问、switch、try/catch、同步方法。自动筛选会跳过这些方法；显式指定则报错。第一阶段建议只选择自有、纯整数计算方法。

生成的源码参与 SHA-256 缓存标识；不同方法实现不会复用同一份原生壳。APK 的包名、方法名称和签名发生变化后，应更新白名单。需要重新构建桌面工具或加固 JAR，旧版工具不会识别新配置。

## 验证

运行 `gradlew :dealJiagu:verifyD2c`，自动编译示例 Java、调用本机 Android SDK 的 D8，验证 native 声明无 code_item、保留未选方法、SHA1/Adler32、拒绝不支持的规则与方法。需要 SDK platform 33、build-tools 和 JDK 17。示例输出在根目录 `build/d2c/generated/`，测试入口在 `src/test/fixtures/d2c/D2cSmoke.java`。

2026-11-09 验证结果：

- 示例 8 个方法完成 D2C 转换，回归任务通过；同时验证含参数调试信息的 DEX，防止 native 方法被写入无效 code_item。
- x86、x86_64、arm64-v8a、armeabi-v7a 生成代码独立编译及与壳合并编译通过。这不扩大第三代 ART Hook 原有的 ABI 支持范围。
- Android 17/API 37 x86_64 ART，开启 CheckJNI，分别执行原 DEX 与转换后 DEX，100 组边界输入的结果一致，除零异常、重载和循环通过。记录保存在 `build/d2c/art-results.txt`。
- 默认主包名筛选已验证子包包含、相似包名排除、不支持方法跳过、显式选择优先及零匹配处理。
- 尚未用真实业务方法验证 D2C 加固 APK 的完整启动流程，未生成新的 Keep D2C 包。x86/arm64 原生执行也尚未实机验证。
