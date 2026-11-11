# 保护报告、启动统计与兼容性回归

## 每包保护报告

加固成功后，在实际输出 APK/XAPK 旁生成 `<输出文件>.protection.json` 和 `.protection.md`。
JSON 包含 D2C/VMP 方法清单、默认选择时跳过 Native 转换的原因、第三代抽离的唯一 code_item、包体增量、构建阶段耗时、壳缓存命中、签名状态及输出 SHA-256。`resources` 记录受完整性保护的文件数量/字节数、路径混淆开关与实际重命名数量（内容加密前统计）。`resourceEncryption` 记录实际加密文件/字节数、启动依赖保留清单、大小/类型预算跳过原因及私有明文缓存的限制。跳过 D2C/VMP 的方法仍可能采用第三代抽离；共享同一 code_item 的方法不重复计数。显式配置中的无效规则继续按原有规则报错。

XAPK 的 `baseProtection` 描述主 APK 的处理结果；外层大小和 SHA-256 对应最终 XAPK，`preservedSplits` 列出保留的 split。主 APK 资源纳入完整性保护并支持可选路径混淆与轻量内容加密；主包业务 SO 默认完整性保护并支持可选内容加密（nativeProtection）；split 内的代码/资源/SO 保持原内容，主包被 split 引用的库保留明文。

报告仅保存在本地，不写入 APK，不包含 AES 密钥、签名密码或完整性私钥。方法名属于源码信息，分享报告前应按业务需要处理。报告记录加固当时的输出；之后单独签名或修改包会改变摘要，需重新核对。生成报告不代表设备实测，字段 `deviceValidation` 保持 `not_run`。

## 启动性能

每个进程通过 Logcat 标签 `NDK_JIAGU_PERF` 输出 JSON，包含 PID、API、阶段、成功/失败及耗时。`attach` 记录壳 attach 完成，`application` 记录原 Application 初始化结束。失败记录异常类型，保留原有异常处理行为。

`phasesMs` 中的 `integrity_ms`、`so_prepare_ms`、`so_load_ms`、`native_attach_ms` 分别表示完整性检查、SO 准备、加载及 Native attach。`framework_provider_gap_ms` 表示壳 attach 结束至壳 onCreate 开始的区间，可能包含 Provider 初始化。`application_context_and_oncreate_ms` 包含上下文替换与原 Application onCreate；其中 `delegate_oncreate_ms` 是嵌套耗时，不能重复相加。`resource_prepare_ms` 包含缓存摘要核对、首次解密/资源重建与挂载；`native_prepare_ms` 为业务 SO 解密/缓存核对及路径接入；`so_cache_hit`、`resource_cache_hit`、`native_cache_hit` 为 0/1 状态值，单位不是毫秒。

`elapsedMs` 从壳 attachBaseContext 开始计时，不包含创建进程之前的系统工作，也不表示 Activity 首帧。指标会受完整性检查、业务初始化、设备负载和缓存状态影响。

v3_13 起，受签名保护的载荷索引 `assets/jiagu-payload.bin` 定位合并 DEX 中的业务载荷。DEX 按流读取，并在 EOF 校验其完整 SHA-256；校验通过前不会把业务 DEX 交给 ART。其余受保护条目仍先行验证，减少大型 APK 的重复解压。因此 DEX 内容校验耗时计入 `native_attach_ms`，不再计入 `integrity_ms`，跨版本比较时需要合并两个阶段。

API 29+ 按 DEX 创建属于实际应用 ClassLoader 的内存元素；仅当 ART cookie 确认已复制到其他映射时释放输入，保留 ART 映射及独立的方法恢复记录。原 Application.attach 更换 ClassLoader 后，组件创建使用更新后的加载器。较旧 API 的加载路径仍需分别做设备回归。

v3_15 起，更换 ClassLoader 时逐个复制 DEX，并把抽离指令区域重置为原保护文件的零值，保持原校验和；不会修改 ART 正在使用的映射。Application 上下文替换同时迁移此前注册的 ServiceConnection 和 BroadcastReceiver 及其调度器上下文，避免 Provider 初始化时注册、Application.onCreate 后解绑失败。全保护设备夹具包含这两类启动边界回归。

## 主机自动回归

在项目根目录执行：

```powershell
.\gradlew.bat :dealJiagu:verifyCompatibility --continue --console=plain
```

汇总 D2C、AES、多 DEX 混合载荷、完整性签名及篡改检测、本地 Gradle 选择、资源表/文件路径混淆、资源内容加密和缓存/预算、四 ABI SO 依赖分析/内容加密和缓存认证、XAPK 跨 split SO 依赖保留/加固与同证书签名检查。输出 `build/compatibility/host-results.json` 和 `.md`。未执行或因前置失败跳过的任务不会记为通过。可按本机环境添加 `-g`、`--offline`。

## 指定设备自动回归

启动本机 ADB 服务并连接设备，显式指定序列号：

```powershell
.\gradlew.bat :dealJiagu:verifyDeviceCompatibility -PdeviceSerial=emulator-5554 --console=plain
```

使用独立双 DEX 包 `com.wpf.jiagu.compat` 验证 D2C、VMP、第三代还原、跨 DEX 调用、资源混淆及内容加密后的字符串/布局/PNG 像素/Drawable/raw/assets/openFd、配置上下文和大文件回退、原 Application、主/远程进程 Provider 和 Activity。同时验证原 Application.attachBaseContext 中的加密业务 JNI、DT_NEEDED 依赖和 JNI_OnLoad 动态注册（extractNativeLibs=false）。覆盖首次安装启动、壳/业务 SO/资源缓存复用、三类缓存损坏恢复、多进程启动、同证书覆盖安装，并记录启动统计与 `am start -W` 的命令耗时。API 21–23 使用安装会话和旧版进程查询命令；框架提前返回 Activity 启动结果时，有界等待实际 Application 及 Activity 验证日志。

脚本拒绝覆盖预先存在的同名测试包，仅操作本次安装的测试包，结束后卸载它；不清空设备 Logcat，不修改业务 App。损坏缓存测试只通过 run-as 修改这个可调试测试包的 SO/资源缓存。设备需 API 21+ 且 ART 版本受当前还原实现支持。

结果与日志保存于 `build/compatibility/device-<序列号>/results.json` 和 `evidence.log`。失败会使 Gradle 任务失败；清理失败也会记录。此结果只证明指定设备上的独立测试包，实际业务 APK、其他 API/ABI/OEM 仍需单独验证。重复运行会覆盖该设备的上一轮报告，请在需要时归档。
