# 热门应用加固签名模拟器测试（2026-11-11）

两批 20 个应用的完整结果、酷狗组件工厂修复与 B站 v3_16 复测，统一见[总报告](POPULAR_APPS_ALL_TEST_RESULTS.md)。下文保留第一批 r8 的原始结论与历史证据。

默认保护批次 `hardened-2026-11-11-r8`，壳版本 v3_15。10 个 APK 均完成加固、指定证书签名、原包/输出 SHA-256 核对、包名版本核对、apksigner 和 16 KiB 对齐检查。最终设备回归：**6/8 通过，2 未通过，0 未完成；另有 2 个此前确认的重签名阻塞**。完整复测和失败对照已完成，尚未达到所有应用通过；总计 6 个通过，3 个指定证书兼容性阻塞，1 个模拟器原版基线崩溃阻塞。

| 应用 | 版本 | 最终结果 | 冷启动 | 前台/恢复 | 产物 |
| --- | --- | --- | --- | --- | --- |
| 微信 | 8.0.80 | 通过 | 3/3 | 通过 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/01-wechat_jiagu_sign.apk) |
| QQ | 9.3.80 | 通过 | 3/3 | 通过 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/02-qq_jiagu_sign.apk) |
| 抖音 | 32.2.6 | 通过 | 3/3 | 通过 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/03-douyin_jiagu_sign.apk) |
| 哔哩哔哩 | 9.18.0 | 通过 | 3/3 | 通过 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/04-bilibili_jiagu_sign.apk) |
| 高德地图 | 17.03.0.2008 | 重签名阻塞 | — | 未执行 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/05-amap_jiagu_sign.apk) |
| 百度地图 | 22.1.20 | 通过 | 3/3 | 通过 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/06-baidumap_jiagu_sign.apk) |
| 网易云音乐 | 9.6.22 | 重签名阻塞 | — | 未执行 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/07-netease_jiagu_sign.apk) |
| 淘宝 | 10.69.0 | 通过 | 3/3 | 通过 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/08-taobao_jiagu_sign.apk) |
| 支付宝 | 12.12.50.8000 | 未通过：原版也崩溃 | 0/3 | 未通过 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/09-alipay_jiagu_sign.apk) |
| WPS Office | 14.36.0 | 未通过：签名拒绝 | 签名错误页 | 未通过 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/10-wps_jiagu_sign.apk) |

本报告仅采用用户确认其他任务曾操作模拟器、要求重新测试后，恢复同一 AVD 并启动的 final5 完整批次，不合并此前各轮通过项。前几轮的前台切换、休眠、ADB 超时及真实 ANR 全部保留。final4 又发生窗口服务无响应与截图损坏，普通重启命令也超时；已通过端口和运行元数据确认只恢复 Pixel_9a/emulator-5554，保留 userdata，跳过旧快照并使用软件渲染。恢复日志见批次 emulator-recovery 目录。返回桌面使用有界的 ActivityManager HOME 启动命令，每次命令结果单独保存。每项原始来源见设备汇总的 sources 字段。

## 设备与范围

使用 emulator-5554：Android 16 / API 36，x86_64 系统，ABI 列表为 x86_64,arm64-v8a，native bridge 为 libndk_translation.so，4 KiB 页，内存约 2.5 GiB。包管理器报告 primaryCpuAbi=arm64-v8a，壳实际 processAbi=x86_64；因此这是带 ARM 翻译的 Intel 模拟器证据，不代表真实 ARM64 设备通过。

最终 8 个重测样本均重新安装本批 APK。完整通过标准为 3 次冷启动、连续前台停留 30 秒（每 5 秒检查 PID 与前台）、3 次返回桌面再恢复；冷启动失败即停止该项并执行原版对照。检查主进程存活、前台 Activity、Java/原生崩溃和 ANR；错误页不能算通过。系统权限提示采用拒绝，并记录实际按钮；不点击应用协议、登录、付款或账号操作。启动耗时不是首帧指标。这些结果只覆盖启动及短时稳定性，不等同于全部业务或长期稳定性通过。较旧 API 的独立加载路径未在本轮重新做设备回归。

此前轮次曾出现截图停留在旧画面及黑屏条纹，相关证据保留在原目录。final5 在恢复模拟器后重新采集截图，并逐一核对 8 个样本的首次冷启动画面；6 个通过项显示各自的欢迎页或协议页，支付宝停留在启动画面且日志已记录崩溃，WPS 显示签名错误页。本轮未复现旧画面及黑屏条纹。截图仅证明当时可见界面，未验证协议后的功能。运行判定同时核对安装结果、实际进程、Activity、UI XML 及崩溃/ANR 日志。WPS 旧脚本的 3 次冷启动记录虽标记进程与前台通过，但错误页无效，最终恢复检查亦失败；已修正脚本，明确的应用签名错误弹窗直接判失败，补充仅重签名对照使用修正后的脚本。

## 已修复的问题

- 壳通过 annotation 实现依赖带入 Kotlin 运行时，覆盖宿主混淆后的 Kotlin 类；改为 compileOnly，壳 DEX 从约 1.9 MiB 降至约 45 KiB。
- 大型合并 DEX 触发 Java 512 MiB 数组分配、原生完整载荷重复保留及并发启动内存压力；改为有界流读取、逐 DEX 加载，并在确认 ART 已复制后释放输入。
- ART DEX 注册到错误 ClassLoader；API 29+ 为实际宿主加载器创建内存元素，更换加载器时创建独立元素。
- WPS 的 RePlugin 在 Application.attach 更换加载器后，组件工厂仍使用旧加载器；刷新组件创建使用的加载器。
- B站更换 ClassLoader 时，ART 映射已恢复部分方法，重新打开会出现 DEX 校验和不匹配；逐个重建保留零值抽离区的保护快照，维持原校验和，不修改原 ART 映射。
- Provider/attach 阶段注册的 ServiceConnection、BroadcastReceiver 在替换 Application 后仍以旧 Context 为键；迁移回调表及调度器上下文，修复抖音解绑 Service 崩溃。API 36 所需字段 ID 在独立 JNI 线程解析，不修改全局 hidden-API 策略。全保护夹具加入提前注册、重新绑定/解绑和更换 ClassLoader 的验证。
- DEX 完整性校验并入载荷流，在交给 ART 前完成 SHA-256 检查；其他条目先行验证。方法恢复只保留独立头部、类定义和指令记录。
- 可选资源内容加密误加密新载荷索引；索引保持可读并继续受签名完整性保护。修复后独立全保护设备测试通过。

默认热门样本保留 DEX 加密、默认 D2C/VMP 选择、方法抽离、资源和 SO 完整性校验；资源路径混淆、资源内容加密与 SO 内容加密未开启。每包实际方法覆盖见同名 protection 报告。独立全保护夹具另行覆盖资源/SO 内容加密。

## 重签名阻塞及对照

高德原版通过 3 次冷启动；仅用指定 JKS 重签名、不加固的包进入 CpuArchErrorActivity，日志为 libserverkey-debug.so 缺失。加固包也出现相同错误。用户已确认记录阻塞。证据：[仅重签名对照](../build/compatibility/popular-apps/sign-only-control/emulator-5554-2026-11-11/results.json)。

网易云原版通过 3 次冷启动、30 秒前台及 3 次恢复；仅重签名包和加固包都在加载 libnesec-x86.so 后主动以 208 退出。不能将其计为通过或归因于壳。继续验证需要可接受该签名的发行商测试包或原签名配置。证据：[仅重签名及原版对照](../build/compatibility/popular-apps/sign-only-controls-r2/emulator-5554-2026-11-11/results.json)。

抖音仅重签名对照通过扩展检查。早期加固失败包含真实崩溃/ANR；其中一轮同时出现其他应用抢占前台，失败记录完整保留，没有按最终通过结果改写历史。

WPS 加固包在 final5 显示“应用签名不正确，请确认安装包是在正规应用商店下载？”，并在恢复时返回桌面。仅用指定 JKS 重签名、不加固的对照包复现同一错误提示；原版通过完整扩展检查。因此 WPS 也属于证书兼容性阻塞，不能通过修改加固层解决。此前仅见进程存活的记录不能算应用可用。

支付宝加固包在 final5 首次冷启动发生 HomeGrid 线程 SIGSEGV，并伴随输入 ANR；原版对照在恢复阶段连续发生相同 SIGSEGV。补充仅重签名包和新装原版也未通过。原生栈均包含 libndk_translation.so 的 DoJavaVMTrampoline_AttachCurrentThread，ARM 栈包含同一 BuildId 的 libmpaas_clientdatabase.so、JNI_OnLoad+144 和 libnative_bridge_vdso.so。当前模拟器没有稳定原版基线，不能归因于加固，也不能记为通过；需在可运行原版的真实 ARM64 设备或兼容系统上继续比较。[支付宝/WPS 补充对照](../build/compatibility/popular-apps/sign-only-controls-r3/emulator-5554-2026-11-11-final5-control/results.json)。

继续消除阻塞所需条件：高德、网易云与 WPS 需要可接受指定证书的测试包或原签名配置；支付宝需要原版可稳定运行的测试环境。本轮未修改这些商业应用的签名校验逻辑。

## 验证证据

- [最终设备汇总](../build/compatibility/popular-apps/hardened-2026-11-11-r8/device-results-final.json)：逐应用证据来源、PID、Activity、耗时、稳定性采样和实际 APK 哈希；各原始目录保存安装日志、Logcat、截图与 UI XML。
- [证据一致性核验](../build/compatibility/popular-apps/hardened-2026-11-11-r8/final-validation.json)：核对原包/产物哈希、结果原始来源、构建与验证源码快照及阻塞对照；evidence_verified 不表示所有应用运行通过。
- [主机结果](../build/compatibility/popular-apps/hardened-2026-11-11-r8/final-host-results.json)：passed，10 项。
- [独立全保护设备结果](../build/compatibility/popular-apps/hardened-2026-11-11-r8/synthetic-final/results.json)：passed，7 个场景，结束清理=passed。
- [构建与签名清单](../build/compatibility/popular-apps/hardened-2026-11-11-r8/results.json)、[壳源摘要](../build/compatibility/popular-apps/hardened-2026-11-11-r8/tooling.json)及每包保护报告。证书 SHA-256：`78bd7b107b4e124bf603ec055eb2ee214fa2865f467cf2ea979661b99585f3f5`。
- [原生方法恢复回归](../build/compatibility/popular-apps/method-restore-r7.txt)：模拟器原生执行，含原输入释放后独立元数据检查、部分方法恢复后重建保护快照及活跃映射不变检查。
- 失败轮次保存在 hardened-2026-11-11、r2 至 r8 的各自测试目录；资源索引失败另存 r6/synthetic-index-failure，回调表直接反射失败另存 r7/synthetic-reflection-failure。r8 初次扩展回归在并行构建时仍记录了抖音启动 ANR，保留该性能失败；最终轮在构建结束后单独执行。build/ 被 Git 忽略，完整日志和 APK 仅保存在当前工作区。

## 复测

在项目根目录用不同的 POPULAR_TEST_ATTEMPT 保留新一轮证据：

~~~powershell
$env:POPULAR_RUN='hardened-2026-11-11-r8'
$env:POPULAR_TEST_ATTEMPT='repeat-1'
$env:POPULAR_EXTENDED='1'
node build/compatibility/popular-apps/test-emulator.mjs 01-wechat 02-qq 03-douyin 04-bilibili 06-baidumap 08-taobao 09-alipay 10-wps
~~~
