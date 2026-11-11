# 热门应用加固测试原始样本（2026-11-10）

两批共 20 个应用的加固、指定证书签名、模拟器结果及失败对照已统一记录在[测试总报告](POPULAR_APPS_ALL_TEST_RESULTS.md)。

已下载 10 个独立 APK，合计 **2,271,120,639 字节（2.12 GiB）**。全部通过 `apksigner verify --verbose --print-certs`，并包含 `arm64-v8a` 原生库。包名、APK 内版本、单包格式、DEX、下载长度与哈希均已记录。

本文记录原始样本的下载与静态核验；后续加固、签名和模拟器验证单独记录，不沿用此前合成测试应用的真机通过结论。

2026-11-11 已另行完成这 10 个样本的默认加固和指定证书签名，并多轮修复、重新加固和执行模拟器测试。最终批次为 [r8 加固签名产物](../build/compatibility/popular-apps/hardened-2026-11-11-r8/README.md)，逐应用结果、重签名阻塞和验证范围见[模拟器测试报告](POPULAR_APPS_EMULATOR_TEST_RESULTS.md)。

## 样本

| 应用 | APK 内版本 | 包名 | 文件 |
| --- | --- | --- | --- |
| 微信 | 8.0.80 | `com.tencent.mm` | `01-wechat.apk` |
| QQ | 9.3.80 | `com.tencent.mobileqq` | `02-qq.apk` |
| 抖音 | 32.2.6 | `com.ss.android.ugc.aweme.mobile` | `03-douyin.apk` |
| 哔哩哔哩 | 9.18.0 | `tv.danmaku.bili` | `04-bilibili.apk` |
| 高德地图 | 17.03.0.2008 | `com.autonavi.minimap` | `05-amap.apk` |
| 百度地图 | 22.1.20 | `com.baidu.BaiduMap` | `06-baidumap.apk` |
| 网易云音乐 | 9.6.22 | `com.netease.cloudmusic` | `07-netease.apk` |
| 淘宝 | 10.69.0 | `com.taobao.taobao` | `08-taobao.apk` |
| 支付宝 | 12.12.50.8000 | `com.eg.android.AlipayGphone` | `09-alipay.apk` |
| WPS Office | 14.36.0 | `cn.wps.moffice_eng` | `10-wps.apk` |

应用来自官网下载入口/公开配置或官方 CDN。京东官网链接返回 HTTP 403，应用宝下载未完成，最终使用支付宝官网的 64 位样本补足 10 个；京东未计入有效样本。

抖音官网此次提供 `.mobile` 包名变体，区别于常见的 `com.ss.android.ugc.aweme`，必须按该变体判读后续测试。官网链接可能保留旧版，本表记录 APK 内实际版本，不承诺最新版或热度排名。

## 本地产物

以项目根目录为基准：

- [原始 APK 目录](../build/compatibility/popular-apps/originals/)：10 个未经本项目加固和重签名的样本。
- [完整样本清单](../build/compatibility/popular-apps/inventory.json)：包名、版本代码、最低/目标 SDK、ABI、DEX 数、字节数、SHA-256、MD5、签名证书 SHA-256、签名方案、来源页面、请求与最终下载地址。
- [采集说明](../build/compatibility/popular-apps/README.md)：大小和 ABI 汇总、核验范围、复核命令。
- `build/compatibility/popular-apps/verification/`：每包下载记录、aapt2 元数据/警告、签名验证输出及结构化元数据。
- `build/compatibility/popular-apps/sources/`：来源页面、公开配置及下载链接证据。

`build/` 被 Git 忽略，APK 和详细日志仅保存在当前工作区；本文保留可追踪索引。原始包保持不变，后续处理产物应使用独立目录。

## 核验边界

- ARM64 结论来自 APK 的 `lib/arm64-v8a/`，不代表已通过 ARM64 手机运行测试。
- 签名校验验证本次 APK 的完整性；记录的证书指纹作为处理前基线，未与独立发行商证书库交叉认证。
- 微信的 aapt2 元数据读取有资源警告，已保存完整日志；aapt2 与签名校验均退出成功。
- 后续应比较原始包与处理后包的冷启动、资源、JNI、组件及恢复行为；第三方应用可能检查自身签名，业务拒绝需要结合原始包对照判断。

重新核验（项目根目录）：

```powershell
node build/compatibility/popular-apps/verify-samples.mjs
```
