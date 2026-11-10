# ARM64 真机加固签名测试结果（2026-11-10）

用户手机上四组测试应用分别完成完整回归，合计 **832 项通过、800 次冷进程启动**。结果由完整矩阵中通过的 resources/native/full 三组与 code 单组完整补测合并；完整矩阵源报告仍为 failed，不表述为一次完整矩阵运行成功。

## 环境与产物

- 机型：OnePlus PLQ110。
- 系统：Android 17 / API 37；ABI 列表仅 `arm64-v8a`；内存页大小 4096 字节。
- 系统指纹：`OnePlus/PLQ110/OP6113L1:17/CP2A.260605.016/B.30b7e77-a6bb2c-a8f423:user/release-keys`。
- 使用此前同一批 12 个签名 APK，没有重新生成或修改 APK。两次采用结果所关联的 24 份 APK 快照均核对 SHA-256，与 `inventory.json` 一致。
- 所有采用的加固 Activity 场景均由壳日志确认实际进程为 `arm64-v8a`，并通过 Application、Provider、Activity 及对应资源/JNI 断言。

## 结果

| 组别 | 通过场景 | 原始/加固冷启动 | 最终清理 | 证据 runId |
| --- | --- | --- | --- | --- |
| code | 207 | 各 100 次 | 通过 | c7362697809c422495f4f2b1b49df54f |
| resources | 208 | 各 100 次 | 通过 | 861d30f5b8a746b384feaeba28d05186 |
| native | 208 | 各 100 次 | 通过 | 861d30f5b8a746b384feaeba28d05186 |
| full | 209 | 各 100 次 | 通过 | 861d30f5b8a746b384feaeba28d05186 |

每次 Activity 启动执行 1,000 轮计算校验。另通过 Activity 重建、远程 Provider、原始包→加固包同版本覆盖及旧数据保留、壳缓存损坏恢复、适用组的资源/JNI 缓存损坏恢复、加固包 v1→v2 同证书升级及数据保留。四组最终均卸载成功，额外查询确认无同名测试包残留。

## 测试器适配及保留的失败记录

首轮 `e60ee00283fd46dd9b2c3cecf190b0a3` 状态为 failed，308 项通过：code 初次安装收到 `Failure [-99]`；resources/full 已通过加固启动及覆盖保留数据，但 `pm clear` 被 `CLEAR_APP_USER_DATA` 权限异常拒绝；native 在覆盖后启动断言超时且没有取得该 PID 的完成日志，不能确定具体原因。用户随后确认手机已解锁。该轮结果没有计入上表完整通过结果，原报告和日志保留。

针对明确的清数据权限异常，测试器现先卸载本轮拥有的测试包，核对包已移除，再安装相同加固 v1 准备空数据环境；其他清数据错误仍失败。此步骤位于两项升级保留数据测试之间，不替代升级断言、不修改手机安全设置。四组报告均记录 `freshDataReset=uninstall_reinstall_after_clear_permission_denied`。

第二轮 `861d30f5b8a746b384feaeba28d05186` 状态为 failed，811 项通过，其中三组完整通过。code 在 `hardened/repeated_cold_83` 超时：`pidof` 返回 `8350 8336`，旧测试器选取首个 PID 8350，没有取得断言，但前台仍是测试 Activity，没有取得应用崩溃证据。测试器增加 ActivityManager `ProcessRecord` 消歧，不能唯一确定组件所属进程时仍明确失败。之后 code 使用同一 APK 完整补测 207 项通过、任务 `BUILD SUCCESSFUL`；补测没有再次命中多个 PID 的分支，因此该分支的真机命中尚未直接验证。不能仅凭补测通过认定首轮 native 超时的根因也已确定。

## 可复核证据

根目录 `build/compatibility/matrix/` 下：

- `phone-arm64-summary.json`：核对四组完整结果、800 次冷启动、ARM64 架构证据及 APK 快照后生成的合并摘要，`scope=profile_aggregate`。
- `device-3B161P00BB000000/861d30f5b8a746b384feaeba28d05186/`：resources/native/full 完整结果和 code 失败记录。
- `device-3B161P00BB000000/c7362697809c422495f4f2b1b49df54f/`：code 完整补测结果。
- `device-3B161P00BB000000/e60ee00283fd46dd9b2c3cecf190b0a3/`：首轮原始失败记录。

每次运行均有 `results.json`、`results.md`、`evidence.log` 及 `apks/` 实际测试快照。复测命令为：

```powershell
.\gradlew.bat :dealJiagu:verifyCompatibilityMatrix -PdeviceSerial=3B161P00BB000000 -PmatrixReuse -PmatrixIterations=100
.\gradlew.bat :dealJiagu:verifyCompatibilityMatrix -PdeviceSerial=3B161P00BB000000 -PmatrixReuse -PmatrixIterations=100 -PmatrixProfiles=code
```

本结果仅覆盖该手机系统版本上的合成测试应用。其他 ARM64 机型、Android 版本、16 KiB 页设备、真实业务和长期运行仍需分别验证；启动耗时也不等同于首帧性能。
