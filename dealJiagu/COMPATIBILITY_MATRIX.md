# 多应用加固与签名测试

生成四个独立包名的测试应用，每组包含原始签名 v1、加固签名 v1、加固签名 v2，共 12 个 APK。原始包与加固包使用相同代码及测试证书，v2 提高 versionCode，便于验证真实覆盖升级。

| 文件前缀 | 包名后缀 | 加固项 |
| --- | --- | --- |
| code | matrix.code | D2C、VMP、多 DEX 方法抽离、完整性校验 |
| resources | matrix.resources | 上述代码保护 + 资源路径混淆、资源内容加密 |
| native | matrix.jni | 代码保护 + JNI SO 加密、依赖库加载 |
| full | matrix.full | 上述全部保护 |

包名前缀为 `com.wpf.jiagu.compat`。Java 类和 JNI 注册符号也随包名生成，确保每个应用实际进入本包代码抽离范围。所有应用均验证 Application、主/远程 ContentProvider、Activity、classes2.dex、反射、布局、图片、raw、assets、文件描述符读取；native/full 另含动态 JNI 注册、静态 JNI 符号和跨库依赖。

## 构建

在项目根目录运行（构建进程使用 JDK 17；同时需要本项目已配置的 JDK 8 工具链）：

```powershell
.\gradlew.bat :dealJiagu:prepareCompatibilityMatrix
```

沿用项目测试环境要求：`local.properties` 的 `sdk.dir`，Android platform 33、build-tools 35.0.0、NDK 26.3.11579264。APK 的 minSdk=21、targetSdk=33，JNI 产物包含 armeabi-v7a、arm64-v8a、x86、x86_64。声明或打包这些 ABI 不等于已在全部 ABI 设备验证。

产物在 `build/compatibility/matrix/`：

- `compatibility-test-apks.zip`：包含以下 12 个 APK、清单、加固报告和说明的测试分发包。
- `{组名}-v1-baseline.apk`：原始签名包，可先单独安装检查基础环境。
- `{组名}-v1-hardened.apk`：加固后签名包。
- `{组名}-v2-hardened.apk`：用于同证书升级和数据保留测试。
- `inventory.json`：本次生成状态、文件大小、SHA-256、证书摘要及保护开关。只有 `status=passed` 表示全部生成成功。
- `*.protection.json`：加固报告，记录 D2C/VMP、跨 DEX 抽离及资源/SO 实际加密清单。

使用仓库已有测试证书 `jiagulibrary/jiagu.jks`，用于本地测试。测试包启用 debuggable，供缓存故障恢复验证使用。

## 设备测试

先启动 ADB server，再选择明确的设备 serial。默认每个应用的原始签名包与加固签名包各执行 20 次冷进程启动：

```powershell
.\gradlew.bat :dealJiagu:verifyCompatibilityMatrix -PdeviceSerial=emulator-5554
```

复用已经生成并通过完整性检查的 12 个包，或增加重复次数：

```powershell
.\gradlew.bat :dealJiagu:verifyCompatibilityMatrix -PdeviceSerial=emulator-5554 -PmatrixReuse -PmatrixIterations=100
```

连接中断后，可用 `-PmatrixProfiles=full` 单独完整重测指定组（支持 code、resources、native、full，多个组用逗号分隔）。报告显式记录所选组；原失败报告继续保留，单组通过不表示一次完整矩阵运行通过。ADB Socket 连接异常归类为 `adb_transport_failure`，仍使任务失败，不自动重试或跳过断言。中断后若清理失败，应先确认残留安装属于该次测试，再手动清理对应测试包。

测试会验证安装签名、重复冷进程启动、每次启动 1,000 轮计算结果、Activity 重建、远程 Provider 进程、原始包到加固包同版本覆盖、SO/资源解密缓存损坏恢复、加固包 v1 到 v2 升级及 SharedPreferences 数据保留。首次加载和后续缓存启动分别记录。升级测试要求旧数据确实存在，不能通过重新创建缺失数据蒙混过关。

部分真机禁止 ADB 调用 `pm clear`。仅在明确收到 `CLEAR_APP_USER_DATA` 权限异常时，测试器会卸载本轮拥有的测试包、确认包已移除，再安装相同加固 v1，继续空数据测试，并在报告记录 `freshDataReset`。此步骤发生在原始包→加固包保留数据验证之后，v1→v2 保留数据验证之前，不替代两项升级断言，也不修改手机安全设置。真机测试期间应保持解锁，并允许系统弹出的测试包安装确认。

每次运行的报告保存在 `build/compatibility/matrix/device-{serial}/{runId}/`，包括 `results.json`、`results.md` 和 `evidence.log`。JSON 关联实际 APK 哈希，分别统计原始/加固重复启动的 TotalTime 中位数和 P95。Activity 断言使用每次启动独立 token，避免旧日志被当成本次通过。

真机上 `pidof` 可能返回多个同名进程。测试器会根据 ActivityManager 的 Activity/Provider `ProcessRecord` 确认实际组件所属 PID，并保存消歧记录；无法唯一确认时明确失败。加固启动结果另记录壳日志中的 `processAbi`，用于核对实际运行架构。

稳定性测试应使用独立设备或模拟器，避免其他安装、自动化或桌面应用抢占前台。每次启动会清理该测试应用的旧任务；若 Application 已完成但 Activity 断言超时，会记录当时前台组件，检测到其他组件占用时报告 `test_environment_interference`。此情况仍使任务失败，原有崩溃和超时断言继续保留。失败日志同时保存壳的原生诊断，便于区分符号、跳板与页权限问题。

脚本只操作上述四个测试包；已存在同名安装时，该组报告失败并保留原安装。脚本安装的包在测试结束后卸载。四组分别执行，一组失败不妨碍其他组出具结果。

结果仅代表所选设备的系统、ABI 和运行次数。`am start -W` 及壳内 Application 耗时不等同于首帧指标。长期运行、内存增长、真实业务、不同 targetSdk、其他 ROM/Android 版本/ABI 仍需分别测试。
