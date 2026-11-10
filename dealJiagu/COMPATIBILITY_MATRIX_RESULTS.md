# 多应用加固签名测试结果（2026-11-10）

最新 ARM64 真机验证：用户 OnePlus PLQ110、Android 17 / API 37、4 KiB 页，四组分别完整通过，共 832 项、800 次冷启动（含 code 单组完整补测）。实际加固进程均确认 ARM64，测试包已清理。详情及保留的失败记录见 [ARM64 真机测试结果](PHONE_ARM64_TEST_RESULTS.md)。以下为此前 x86_64 模拟器修复验证记录。

已重新生成 12 个签名 APK，四组应用均有原始签名 v1、加固签名 v1、加固签名 v2。分发包为 `build/compatibility/matrix/compatibility-test-apks.zip`，共约 27.5 MiB；已核对 ZIP 内及磁盘上 12 个 APK 的 SHA-256 与 inventory.json 一致，所有 APK 使用同一个测试签名证书。

构建和签名验证通过。已修复导致加固包间歇启动失败的 x86_64 Hook 跳板分配问题。独立模拟器上四组各自完整回归均通过，合计 832 项，包括原始/加固版本各 100 次冷进程启动（四组合计 800 次）。结果由首次运行完成的前三组和连接中断后完整重测的 full 组合并；首次完整矩阵运行仍保留 failed 状态，不视为一次完整矩阵成功。修复前失败记录保留在本文后半部分。

| 组别 | 完整通过场景 | 测试包清理 | 证据 runId |
| --- | --- | --- | --- |
| code | 207 | 通过 | e8b50b5ef4ed4c49958c2ac1b462ea7e |
| resources | 208 | 通过 | e8b50b5ef4ed4c49958c2ac1b462ea7e |
| native | 208 | 通过 | e8b50b5ef4ed4c49958c2ac1b462ea7e |
| full | 209 | 通过 | 187624dba98c4812bd37fc8569b80831 |

## 根因与修复

诊断构建在原失败环境捕获到 `x86 hook allocation failed target=0x7e3afd88f6f0 errno=88`。结合分配器源码及可重复失败用例，确认旧分配器约每隔 16 MiB 提交 mmap 地址提示，会遗漏 ART 附近可用的小空隙；内核忽略已被占用的提示后可能反复返回超出 rel32 范围的地址，导致 DefineClass Hook 安装失败。此日志中的 errno 不能单独解释失败，因为地址范围不合适也会拒绝分配结果。

`jiagulibrary/src/main/cpp/utils/x86_hook.cpp` 现在读取 `/proc/self/maps` 的实际空隙，按距离尝试候选，并验证内核实际返回地址的跳转范围和完整指令重定位。仍使用非固定 mmap，遇到不合适的候选会释放并继续；读取映射失败时保留旧提示策略。另增加分配、重定位范围及页权限失败诊断。

新增原生回归用例：预留 6 GiB 虚拟地址区域，仅在目标约 2 MiB 处留一个小空隙。旧实现安装 Hook 断言失败，修复后能执行替换函数及原跳板。修复版完整 `x86_hook_test` 在独立 x86_64 模拟器连续运行 20 次通过，记录为 `build/compatibility/hook-regression-results.log`。x86 测试目标编译通过，本轮没有在 32 位进程执行。

修复前 JNI 组的 Activity 超时另查到外部前台抢占：系统日志在 07:37:22.502（设备日志时间）记录 `com.example.mylauncher.demo/.MainActivity` 启动，发生于测试 Application 已完成、Activity 断言尚未完成期间。测试器增加启动前清理本应用旧任务、超时保存前台组件以及 `test_environment_interference` 分类，仍按失败处理。本轮使用独立模拟器隔离这种干扰。

## 修复后运行环境与范围

- 设备：独立 `emulator-5580` / `sdk_gphone64_x86_64`，Android 16 / API 36，4 KiB 内存页。
- 系统指纹：`google/sdk_gphone64_x86_64/emu64xa:16/BE4B.251210.005/14574095:user/release-keys`。
- 系统 ABI 列表为 `x86_64,arm64-v8a`；实际验证 x86_64 进程。四种 ABI 壳均重新编译成功，所有四组 v1 加固报告均为 `shellCacheHit=false`、`signed=true`。
- 每组原始签名 v1、加固签名 v1 各执行 100 次冷进程启动，共 800 次；每次 Activity 启动执行 1,000 轮计算校验。
- 另覆盖 Activity 重建、远程 Provider、原始包→同版本加固包覆盖、资源/SO 缓存损坏恢复、加固包 v1→v2 升级及旧数据保留。
- 完整矩阵 runId：`e8b50b5ef4ed4c49958c2ac1b462ea7e`。报告、日志及实际测试 APK 快照位于 `build/compatibility/matrix/device-emulator-5580/e8b50b5ef4ed4c49958c2ac1b462ea7e/`。

该完整矩阵运行最终状态为 failed：code/resources/native 分别完整通过 207/208/208 项并清理成功；full 已通过 116 项，在 `hardened/repeated_cold_13` 的 force-stop 请求处因 `Connection reset` 中断，清理也因连接失败而未完成。中断前最后一轮 Application、Provider、资源、JNI、Activity 断言均成功，没有取得应用崩溃证据。主机 adb 进程启动时间为北京时间 16:18:24，表明测试期间 ADB 服务被重新启动；随后确认独立模拟器仍正常启动、测试包仍存在，再手动卸载该轮遗留的 full 测试包。

原失败报告保持不变，其中旧版测试器将此连接错误记为 `fixture_failure`；新测试器会将 Socket/超时/EOF 异常记为 `adb_transport_failure`，仍让任务失败。新增 `-PmatrixProfiles=full` 支持仅对受中断组完整重测，报告显式记录所选组，默认仍测试全部四组。

使用同一批 APK 的 full 完整重测 runId 为 `187624dba98c4812bd37fc8569b80831`，命令为：

```powershell
.\gradlew.bat :dealJiagu:verifyCompatibilityMatrix -PdeviceSerial=emulator-5580 -PmatrixReuse -PmatrixIterations=100 -PmatrixProfiles=full
```

该重测任务以 `BUILD SUCCESSFUL` 结束，full 全部 209 项通过且卸载清理成功。两次运行的 APK 快照共 24 份文件均逐一校验 SHA-256 与当前 inventory 一致。报告与原始日志位于 `build/compatibility/matrix/device-emulator-5580/187624dba98c4812bd37fc8569b80831/`。分发 ZIP 中同时提供使用说明 `README.md` 和本结果文档 `RESULTS.md`。

## 修复前运行环境与范围

- 设备：`emulator-5554` / `sdk_gphone64_x86_64`，Android 16 / API 36，4 KiB 内存页。
- 系统 ABI 列表：`x86_64,arm64-v8a`，本次在 x86_64 模拟器运行；未进行 ARM 真机验证。
- 应用：minSdk 21、targetSdk 33；每组原始/加固版本各计划执行 20 次冷进程启动。首次数据初始化、缓存复用、Activity 重建、远程 Provider、覆盖升级、缓存损坏恢复独立计入场景。
- 每个 Activity 启动执行 1,000 轮计算校验。首个失败终止该组后续场景，其余组继续运行。
- 两轮均只安装、清理本次测试应用；四组测试包的清理结果全部通过。

## 修复前两轮结果

表中数字是该组已通过的场景数，不是通过率；失败后的剩余场景未执行。

| 组别 | 首轮 | 复测 | 失败现象 |
| --- | --- | --- | --- |
| code | 通过，47 场景 | 失败，已通过 23 场景 | 复测在加固包清空数据后首次启动时，ART DefineClass Hook 初始化失败 |
| resources | 失败，已通过 39 场景 | 通过，48 场景 | 首轮在加固包 repeated_cold_16 启动时，ART DefineClass Hook 初始化失败 |
| native | 失败，已通过 35 场景 | 通过，48 场景 | 首轮在加固包 repeated_cold_12 时，Application 已完成但 Activity 断言超时；后续系统日志确认期间发生外部前台抢占 |
| full | 失败，已通过 36 场景 | 失败，已通过 30 场景 | 首轮 repeated_cold_13、复测 repeated_cold_7 启动时，ART DefineClass Hook 初始化失败 |

四个原始签名包在两轮中均通过各自 20 次冷进程启动、Activity 重建和远程 Provider 场景。code 首轮、resources/native 复测另通过加固包重复启动、对应缓存损坏恢复、Activity 重建、多进程以及 v1→v2 升级并保留旧数据的校验。

明确记录的异常为：

```text
java.lang.RuntimeException: Unable to instantiate application com.wpf.util.jiagulibrary.StubApp
Caused by: java.lang.IllegalStateException:
Class-based method restoration requires ARM/Intel ART API 21+ and a supported DefineClass hook
```

源码入口为 `jiagulibrary/src/main/cpp/jiagu.cpp` 的 `install_class_restore` 检查，以及 `utils/class_restore.cpp`、`utils/x86_hook.cpp` 的 x86 Hook 分支。此 Java 异常是共同的失败出口；后续原生诊断及针对性回归才定位到本文所述的跳板分配问题。

## 修复前可复核证据

首轮（北京时间 15:35:40–15:38:33）：

- `build/compatibility/matrix/device-emulator-5554/659843a8e1a541989e02d025ee6f5c03/results.json`
- 同目录 `evidence.log`、`results.md`、`apks/`，保留首轮实际测试 APK 和清单。

复测（北京时间 15:41:40–15:43:49）：

- `build/compatibility/matrix/device-emulator-5554/baebeff4a0f040bdb2c97f16c07a5425/results.json`
- 同目录 `evidence.log`、`results.md`、`apks/`，保留复测实际测试 APK 和清单。

带原生诊断的修复前复现：`build/compatibility/matrix/device-emulator-5554/276d2fe568ae40c1a8c447962f4a8ca1/`。该轮 code/resources/native/full 均再次失败，进一步取得上述分配失败日志。共享模拟器随后不可用，原生日志摘录来自当时工具输出，未单独保存完整原生日志文件。

报告含每次成功启动耗时和中位数/P95；失败或超时的启动不计入成功耗时样本。本轮修复验证范围为 API 36 x86_64 模拟器；其他 Android 版本、ROM、ABI、16 KiB 页设备、长期运行及真实业务流程没有在本轮验证。

随后尝试启动用户创建的 API 36.1 ARM64 `Pixel_9` AVD：当前 Windows x86_64 主机上的 Emulator 37.2.13 拒绝该客体架构，直接引擎尝试也未进入 Android，因此该模拟器尝试未产生 ARM64 APK 结果。详细日志和状态见 [ARM64 模拟器测试状态](ARM64_EMULATOR_TEST_STATUS.md)。之后已改用用户 ARM64 手机完成上文链接的真机验证。

构建、复测命令及各组覆盖说明见 `COMPATIBILITY_MATRIX.md`。
