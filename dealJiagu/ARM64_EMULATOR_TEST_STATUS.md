# ARM64 模拟器测试状态（2026-11-10）

后续已改用用户 ARM64 手机完成四组完整验证，见 [ARM64 真机测试结果](PHONE_ARM64_TEST_RESULTS.md)。本文仅记录此前模拟器启动尝试。

已找到用户创建的 `Pixel_9` AVD，配置目录为 `C:/Users/1/.android/avd/Pixel_9_2.avd/`，使用已安装的 API 36.1 Google Play `arm64-v8a` 镜像。当前主机为 Windows x86_64，Android Emulator 版本为 37.2.13（build 16740729）。

状态：**模拟器启动失败，ARM64 APK 测试未执行。** 这属于测试环境限制，不能据此判断加固包在 ARM64 上是否通过或失败。

## 标准启动结果

对用户 AVD 使用独立端口 5582、禁用快照和硬件加速，启动器仍以退出码 1 拒绝运行：

```text
FATAL | Avd's CPU Architecture 'arm64' is not supported by the QEMU2 emulator on x86_64 host. System image must match the host architecture.
```

日志：

- `build/compatibility/arm64/pixel9-launcher.stdout.log`
- `build/compatibility/arm64/pixel9-launcher.stderr.log`

## 直接引擎验证

尝试直接运行当前安装的 `qemu-system-aarch64-headless.exe`，仍未进入 Android：

| 启动方式 | 结果 | 日志前缀 |
| --- | --- | --- |
| ARM64 引擎，关闭音频后端 | `PCI bus not available for virtio-snd-pci` | `pixel9` |
| 额外禁用 VirtioSndCard | `PCI bus not available for hda` | `pixel9-nosnd` |
| 尝试覆盖 QEMU 声卡参数 | `Unknown sound card name` | `pixel9-noaudio`、`pixel9-empty-audio` |

上述 stdout/stderr 日志均位于 `build/compatibility/arm64/`。没有获得 ARM64 Android 的 ADB 连接、启动完成标记或应用执行结果；没有安装本轮测试 APK。失败的测试进程已关闭，用户 AVD 的 `config.ini` 未修改。

继续验证需要可运行该镜像的 ARM64 主机，或通过 ADB 连接的 ARM64 真机。届时应先确认实际测试进程为 ARM64，再对同一批 APK 运行矩阵；x86_64 模拟器包含 ARM 转译能力并不等于验证了 ARM64 ART Hook。

已有 832 项通过结果仍仅适用于此前的 Android 16 x86_64 模拟器，详见 [修复与测试结果](COMPATIBILITY_MATRIX_RESULTS.md)。
