# 两批 20 个应用加固、签名与模拟器测试总报告

测试日期：2026-11-11。两批共 20 个不同包名的应用均完成原包核验、项目加固、指定证书签名和必要的运行对照。**加固与签名静态检查 20/20 通过；设备运行 14 个通过、6 个阻塞，未达到全部通过。**第一批为 6 个通过、4 个阻塞，第二批为 8 个通过、2 个阻塞。每个通过项均使用同一次完整设备记录判定：3 次冷启动、30 秒前台观察、3 次返回桌面再恢复，无目标进程崩溃或 ANR。

这是一次包含修复复测的测试活动汇总。表中明确列出实际 APK 的壳版本：第一批以 r8、第二批以 r1 为基线；组件工厂修复后，酷狗使用第二批 r2，B站使用第一批 r9，壳版本 v3_16。其余 v3_15 记录保留原结论，不能当作所有应用已在 v3_16 上重新回归。

## 逐应用结果

| 批次 | 应用 | APK 内版本 | 壳 | 加固/签名静态 | 设备结果 | 冷启动与稳定性 | 文件 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 微信 | 8.0.80 | v3_15 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/01-wechat_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps/hardened-2026-11-11-r8/emulator-5554-2026-11-11-final5/results.json) |
| 1 | QQ | 9.3.80 | v3_15 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/02-qq_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps/hardened-2026-11-11-r8/emulator-5554-2026-11-11-final5/results.json) |
| 1 | 抖音 | 32.2.6 | v3_15 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/03-douyin_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps/hardened-2026-11-11-r8/emulator-5554-2026-11-11-final5/results.json) |
| 1 | 哔哩哔哩 | 9.18.0 | v3_16 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r9/apks/04-bilibili_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps/hardened-2026-11-11-r9/emulator-5554-2026-11-11-factory-final2/results.json) |
| 1 | 高德地图 | 17.03.0.2008 | v3_15 | 通过 | 重签名阻塞：缺 libserverkey-debug.so；原版可启动 | 见对照说明 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/05-amap_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps/sign-only-control/emulator-5554-2026-11-11/results.json) |
| 1 | 百度地图 | 22.1.20 | v3_15 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/06-baidumap_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps/hardened-2026-11-11-r8/emulator-5554-2026-11-11-final5/results.json) |
| 1 | 网易云音乐 | 9.6.22 | v3_15 | 通过 | 重签名阻塞：主动 exit 208；原版完整扩展通过 | 见对照说明 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/07-netease_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps/sign-only-controls-r2/emulator-5554-2026-11-11/results.json) |
| 1 | 淘宝 | 10.69.0 | v3_15 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/08-taobao_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps/hardened-2026-11-11-r8/emulator-5554-2026-11-11-final5/results.json) |
| 1 | 支付宝 | 12.12.50.8000 | v3_15 | 通过 | 环境阻塞：原版、仅签名、加固包均未通过 | 见对照说明 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/09-alipay_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps/hardened-2026-11-11-r8/emulator-5554-2026-11-11-final5/results.json) |
| 1 | WPS Office | 14.36.0 | v3_15 | 通过 | 重签名阻塞：应用明确提示签名不正确 | 见对照说明 | [APK](../build/compatibility/popular-apps/hardened-2026-11-11-r8/apks/10-wps_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps/hardened-2026-11-11-r8/emulator-5554-2026-11-11-final5/results.json) |
| 2 | Firefox | 160.0 | v3_15 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/apks/11-firefox_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/emulator-5554-2026-11-11-final1/results.json) |
| 2 | 美团 | 12.68.402 | v3_15 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/apks/12-meituan_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/emulator-5554-2026-11-11-final1/results.json) |
| 2 | Aegis验证器 | 3.4.3 | v3_15 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/apks/13-aegis_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/emulator-5554-2026-11-11-final1/results.json) |
| 2 | 知乎 | 11.14.0 | v3_15 | 通过 | 重签名兼容性阻塞：项目/SDK 仅签名均崩溃，原版通过 | 见对照说明 | [APK](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/apks/14-zhihu_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/emulator-5554-2026-11-11-final1/results.json) |
| 2 | 酷狗音乐 | 20.8.8 | v3_16 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r2/apks/15-kugou_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r2/emulator-5554-2026-11-11-factory-final2/results.json) |
| 2 | 爱奇艺 | 17.10.7 | v3_15 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/apks/16-iqiyi_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/emulator-5554-2026-11-11-final1/results.json) |
| 2 | 快手 | 14.8.30.50423 | v3_15 | 通过 | 原版基线阻塞：原版与加固重复启动退出 | 见对照说明 | [APK](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/apks/17-kuaishou_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/emulator-5554-2026-11-11-exclusive-final3/results.json) |
| 2 | 腾讯视频 | 9.04.75.32454 | v3_15 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/apks/18-qqvideo_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/emulator-5554-2026-11-11-final1/results.json) |
| 2 | NewPipe | 0.29.3 | v3_15 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/apks/19-newpipe_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/emulator-5554-2026-11-11-final1/results.json) |
| 2 | UC浏览器 | 19.2.7.1563 | v3_15 | 通过 | 启动与短时稳定性通过 | 3/3；30 秒 + 3 次恢复 | [APK](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/apks/20-uc_jiagu_sign.apk) / [证据](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/emulator-5554-2026-11-11-final1/results.json) |

表中的“加固/签名静态通过”包含原包及输出 SHA-256、包名/版本、apksigner 签名和证书核验、zipalign 16 KiB 原生库对齐检查。它与设备运行结果分别判定。高德和网易云使用此前确认的仅重签名阻塞；未对其 r8 APK 再执行一轮完整设备测试。所有“通过”仅指本报告规定的启动与短时稳定性范围。

## 环境与测试范围

- 只使用 Pixel_9a / emulator-5554：Android 16、API 36，x86_64 系统，ABI 列表 x86_64,arm64-v8a，ARM 翻译库 libndk_translation.so，4096 字节内存页。
- 包管理器可能报告 primaryCpuAbi=arm64-v8a，壳日志报告实际 processAbi=x86_64。因此本报告是带 ARM 翻译的模拟器结果，**没有证明这 20 个商业样本在真实 ARM64 或 16 KiB 页设备上通过**。APK 对齐检查不能代替 16 KiB 页设备运行。
- 每项重新安装所选 APK；校验实际安装文件哈希。通过项检查前台 Activity、活跃进程、3 次冷启动和 9 次稳定性观察。明确的签名错误页直接判失败。
- 系统权限提示按脚本拒绝并记录按钮。不点击应用协议、登录、支付等操作。测试覆盖欢迎页、协议页和部分默认首页，不覆盖完整业务、网络服务、登录、播放、支付或长期压力测试。
- 第二批 Firefox、美团、爱奇艺、腾讯视频、UC、酷狗显示正常欢迎/协议页；NewPipe 显示项目提示页。Aegis 的 UI XML 显示正常 Welcome 内容，进程与 Activity 检查通过，但截屏为黑色，视觉结果未确认，不将黑图计为视觉通过。
- 第二批最后一轮酷狗测试前，发现其他应用活动和前台变化，测试进程异常退出，该轮未完成。用户确认其他任务已暂停后，另开 factory-final2 完整复测；中断日志保留，不据此改写前后的结果。
- 第一批为释放模拟器空间，卸载了本任务登记的 10 个测试包；原包、产物和日志均保留。操作记录见 [previous-batch-cleanup.json](../build/compatibility/popular-apps-batch2/previous-batch-cleanup.json)。

## 本轮修复与验证

酷狗 20.8.8 的加固包在安装 CommonProvider 时发生 ExceptionInInitializerError，根因为 IProcessDesc 为空。项目仅重签名、SDK 仅重签名及原版对照通过，确认需要修复加固兼容性。

原包声明 MusicAppAndroidComponentFactory，继承 JanusAndroidComponentFactory；工厂在 Application.attach 阶段接收组件代理，并参与 Provider 创建。系统在解密前无法加载这个工厂，回退到默认工厂。旧壳解密后直接构造 Application，没有恢复原工厂，导致隐私启动路径错误创建业务 Provider。

v3_16 在业务 DEX 可用后、真实 Application.attach 前恢复 LoadedApk 的原组件工厂，并通过该工厂实例化 Application。API 28+ 路径隔离在单独类中；受限制的 framework 字段用现有的独立 JNI 线程方式解析，不修改应用全局 hidden-API 策略。生成的 jiaguLibrary.zip 已同步更新。

新增 FixtureComponentFactory 回归：在 Application.attach 验证工厂已执行，在 Provider.onCreate 验证创建经过原工厂；与已有 ClassLoader 替换、早期回调、资源/SO 内容加密和多进程测试一起验证。直接反射该字段失败的开发轮保留在 factory-reflection-failure，最终采用 JNI 访问后通过。

酷狗重新加固、指定证书签名后通过完整扩展测试；正常进入独立隐私进程 com.kugou.privacy。测试脚本原来只查主包 PID，曾把原版的合法进程切换误判为退出。现已通过前台 ActivityRecord 的包归属、ProcessRecord 和 ps 核对该进程，并将日志中关联的任意名称进程纳入崩溃检查。修正后原版亦完整通过。旧误判记录保留并由本轮对照取代。

验证证据：

- [主机 10 项回归](../build/compatibility/popular-apps-batch2/factory-host-results.json)：全部通过，涵盖 D2C、AES、DEX 流、混合载荷、完整性、Gradle 启动、资源保护/加密、SO 保护和 XAPK。
- [独立全保护设备 7 个场景](../build/compatibility/popular-apps-batch2/factory-synthetic-final/results.json)：全部通过、结束清理通过。这是独立夹具证据，不代替商业应用运行结果。
- [酷狗修复后的完整设备轮](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r2/emulator-5554-2026-11-11-factory-final2/results.json)、[原版完整对照](../build/compatibility/popular-apps-batch2/original-controls/emulator-5554-2026-11-11-baseline-final/results.json)。
- [B站修复后的独立复测](../build/compatibility/popular-apps/hardened-2026-11-11-r9/emulator-5554-2026-11-11-factory-final2/results.json)。

第一批已修复的 Kotlin 运行时冲突、大 DEX 内存压力、ART ClassLoader 注册、RePlugin 加载器替换、方法恢复校验和、早期回调上下文及可选加密索引问题，详见[第一批详细报告](POPULAR_APPS_EMULATOR_TEST_RESULTS.md)。本报告保留这些历史结果，没有将旧轮次的不同通过阶段拼接成一次通过。

## 签名核查与 SDK 通用签名对照

所有加固产物仍使用用户指定的 MyLauncher 发布 JKS。证书 SHA-256：

`78bd7b107b4e124bf603ec055eb2ee214fa2865f467cf2ea979661b99585f3f5`

已检查 PackageSigner 的 zipalign → apksigner → 证书验证流程。本轮未发现需要替换产品签名代码的错误。另建立独立对照，使用 Android SDK 36.1.0 官方 zipalign.exe 与 lib/apksigner.jar，绕过项目 PackageSigner，只对原版 APK 使用同一 JKS 重签名。密码从用户配置读取，仅在进程内及子进程环境传递，报告不记录密码。

| 对照应用 | 项目仅重签名 | SDK 通用仅重签名 | 原版 | 结论 |
| --- | --- | --- | --- | --- |
| 知乎 | 崩溃 | 崩溃 | 完整通过 | 更换证书后在本环境不兼容；通用签名未消除故障 |
| 酷狗音乐 | 完整通过 | 完整通过 | 完整通过 | 原故障来自加固组件工厂恢复，已修复 |
| B站 | 独立复测完整通过 | 完整通过 | 完整通过 | 尚无证据认定项目签名实现存在故障 |

两种签名输出均通过 apksigner 验证，证书一致。文件 SHA-256 不同本身不能证明签名代码错误。知乎的原生栈包含 libndk_translation.so 的 berberis_HandleNoExec / ExecuteGuest；目前只能确认更换证书与本环境运行失败相关，未确认其内部是否主动进行证书自校验。

B站 v3_16 [首轮加固包](../build/compatibility/popular-apps/hardened-2026-11-11-r9/emulator-5554-2026-11-11-factory-final/results.json)出现 `FORTIFY: pthread_mutex_lock called on a destroyed mutex` / SIGABRT；项目仅签名的先前轮也出现同类崩溃及输入 ANR，并在原版对照期间因本机 Node 进程异常退出而未生成完整结果。因此不能将那一轮作为有效通过或签名根因证明。换用 bundled Node v24.22.1 后，[项目仅签名](../build/compatibility/popular-apps/sign-only-controls/emulator-5554-2026-11-11-factory-control3/results.json)和 [SDK 官方仅签名](../build/compatibility/popular-apps/sdk-sign-only-controls/emulator-5554-2026-11-11-factory-control2/results.json)均完成完整通过；同一个加固 APK 在 factory-final2 独立完整复测通过，画面为正常协议页。更换 Node 后测试得以完整执行，但没有证据表明 Node 版本导致或修复了应用内部的互斥锁崩溃。历史故障说明该模拟器曾出现不稳定，最终所选通过也仅证明一次完整短时测试，不证明长期稳定或该故障已根治。

对照来源：[项目控制包清单](../build/compatibility/popular-apps-batch2/sign-only-controls/results.json)、[SDK 控制包清单](../build/compatibility/popular-apps-batch2/sdk-sign-only-controls/results.json)、[知乎项目对照](../build/compatibility/popular-apps-batch2/sign-only-controls/emulator-5554-2026-11-11-control1/results.json)、[知乎 SDK 对照](../build/compatibility/popular-apps-batch2/sdk-sign-only-controls/emulator-5554-2026-11-11-control1/results.json)、[酷狗项目对照](../build/compatibility/popular-apps-batch2/sign-only-controls/emulator-5554-2026-11-11-control2/results.json)、[酷狗 SDK 对照](../build/compatibility/popular-apps-batch2/sdk-sign-only-controls/emulator-5554-2026-11-11-control2/results.json)。这些 runner 记录中的 hardened 字段在控制目录代表仅签名包；original-controls 中代表原版，不能当成加固测试结果。

## 未通过项及继续测试的条件

| 应用 | 可复核的现象 | 对照及继续条件 |
| --- | --- | --- |
| 高德 | 仅重签名和加固包进入 CpuArchErrorActivity，缺 libserverkey-debug.so | 原版 3 次冷启动通过；用户已确认记录阻塞。需要可接受指定证书的测试包或原签名配置。 |
| 网易云音乐 | 加固和仅重签名加载 libnesec-x86.so 后以 208 主动退出 | 原版完整扩展通过；需要可接受指定证书的测试版本。 |
| WPS | 加固和仅重签名明确显示“应用签名不正确” | 原版完整扩展通过；需要可接受指定证书的测试版本。 |
| 知乎 | 加固及两种仅重签名都发生原生 SIGSEGV | 原版完整扩展通过；需要可接受新证书的版本，并在原版可稳定运行的真实 ARM64 环境补充比较。 |
| 支付宝 | 原版、加固、仅重签名均未通过；原生栈涉及 libndk_translation.so 与 libmpaas_clientdatabase.so JNI_OnLoad | 当前没有稳定原版基线；需原版可稳定运行的设备/系统。不能归因于加固，也不能计为通过。 |
| 快手 | 原版及加固重复启动时进程退出；未观察到可归属的 Java/原生崩溃或 ANR | 当前没有稳定原版基线；需在可重复启动原版的环境继续比较。详细启动次数以所选轮次为准。 |

当前未达到“所有应用通过”。这些阻塞有原版或仅重签名对照支持；没有修改商业应用的证书校验或停用默认保护以改变结果。

## 样本、保护配置与证据清单

第一批：微信、QQ、抖音、B站、高德、百度地图、网易云、淘宝、支付宝、WPS。第二批：Firefox、美团、Aegis、知乎、酷狗、爱奇艺、快手、腾讯视频、NewPipe、UC。第二批按可获得且可核验的官方 APK 选择，未按热度排名；下载失败和被替换的候选不计入这 20 个。官网入口可能提供旧版，表中使用 APK 内实际版本。

默认商业样本开启 DEX 加密、默认 D2C/VMP 选择、方法抽离、资源及 SO 完整性保护。资源路径混淆、资源内容加密和 SO 内容加密未开启；每包实际方法覆盖及条目数见同名 .protection.json/.md。独立夹具另外覆盖可选内容加密。

- [两批统一机器可读结果](../build/compatibility/popular-apps-all/results.json)：20 个原包/产物哈希、证书、下载来源、完整所选设备轮及控制来源。
- [统一证据核验](../build/compatibility/popular-apps-all/validation.json)：重新计算全部 20 个原包及产物哈希，核对安装文件、通过项 3 次冷启动与 9 个观察、失败对照、修复后源码快照及主机/夹具结果。evidence_verified 表示证据相符，**不表示 20 个应用全部通过**。
- 补查全部所选通过轮的 Logcat，按进程 PID、包名及进程启动记录关联 `libc Fatal signal`、`FORTIFY`、Java 异常与 ANR；未发现遗漏的目标崩溃。该检查也确认 B站历史失败轮存在 SIGABRT，不能沿用旧 runner 的 crashOrAnr=false 宣称无崩溃。
- [结束时安装记录](../build/compatibility/popular-apps-all/retained-installations.json)：将 B站、知乎、酷狗的对照包替换回所选实际加固包，逐一核对设备上 base.apk 哈希。这一步仅恢复安装，不另算运行通过。
- [第一批原始清单](../build/compatibility/popular-apps/inventory.json)、[第二批原始清单](../build/compatibility/popular-apps-batch2/inventory.json)：原证书、版本、ABI、DEX、下载元数据。其中 not_run 是下载时的静态快照，后续测试状态以本总报告为准。
- [第一批 r8](../build/compatibility/popular-apps/hardened-2026-11-11-r8/results.json)、[B站 r9](../build/compatibility/popular-apps/hardened-2026-11-11-r9/results.json)、[第二批 r1](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r1/results.json)、[酷狗 r2](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r2/results.json)：加固与签名构建清单。
- [修复后源码与 runner 摘要](../build/compatibility/popular-apps-batch2/hardened-2026-11-11-r2/tooling.json)。旧 r8 的 sourceSnapshot=matched 是修复前当时的核验；本轮明确修改了壳及夹具，不能将其解释为旧源码仍与当前源码完全相同。

各设备轮目录保留安装日志、Logcat、Activity、UI XML 与截图；通过项使用一轮完整记录，未覆盖或删除历史失败。build/ 目录被 Git 忽略，APK 与完整原始证据保存在当前工作区。总报告与本轮兼容性修复保留在项目源码中。
