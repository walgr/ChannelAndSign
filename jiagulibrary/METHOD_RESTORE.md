# 第三代方法抽离还原

打包端已接入方法抽离、D2C、基础整数 VMP 的混合保护，主包名默认筛选、精确白名单及支持范围见 `../dealJiagu/D2C.md`。支持的方法按复杂度分配到 D2C/VMP，其余方法继续使用本文的第三代还原。

打包端仍使用 `[4 字节大端指令偏移][4 字节大端字节数][原始指令]`，不改变现有载荷格式。共享同一个 `code_item` 的方法只抽离一次。

壳在解密后解析所有 DEX 的 `class_data_item`，建立每个类与抽离指令的映射，但此时不回填。安装 ART `ClassLinker::DefineClass` Hook 后再创建内存类加载器；定义业务类之前，恢复该类的 direct/virtual 方法，再调用原始 DefineClass。抽象方法和 native 方法没有指令，不做回填。未加载类的指令保持抽离状态；已加载类的方法保留在内存中，不在每次方法调用后擦除。

Hook 根据 ART DexFile 的 `begin_` 找到运行时实际使用的 DEX，以头部和 class_def 双重核对，支持 ART 复制输入缓冲区。各 DEX 独立保存记录，并通过互斥锁串行回填。原始载荷、指令记录和 Hook 持续保留到进程结束，避免延迟加载时访问已释放内存。指令页按系统页大小申请读写权限，不需要执行权限。

当前有抽离指令时支持 **armeabi-v7a、arm64、x86、x86_64，ART API 21+、可解析已列出的 DefineClass 符号**。ARMv7 使用固定提交的 Dobby 静态链接实现（来源、许可与本地修复见 `src/main/cpp/third_party/dobby/UPSTREAM.md`）；arm64 沿用 And64InlineHook；x86/x86_64 使用本地 POSIX Hook 和固定版本 MinHook v1.3.4 的 HDE 指令解码器，无构建期网络依赖。32 位 ART 使用 `lib` 路径和 `size_t=j` 符号，64 位使用 `lib64` 和 `size_t=m`。其他架构、低版本、符号或指令不匹配会报错，不回退到启动时整包还原。OEM ART 和后续 Android 版本仍需实机核对内部 ABI、符号、内存保护及类验证流程，不能仅凭编译认定兼容。

x86/x86_64 Hook 复制完整指令并重定位相对调用、跳转、条件分支；x86 处理 PIC `get_pc_thunk` / `call; pop`，x86_64 处理 RIP 相对寻址并使用附近的跳板。遇到不支持的指令（例如 LOOP/JCXZ、VEX/EVEX）会在改写入口之前失败。Hook 仅在壳初始化时安装一次，不支持并发安装、卸载；入口写入期间要求目标函数没有正在执行的其他线程。此限制需要在实际 ART 启动环境中验证。

`StubApp.VERSION` 已更新为 `v3_9`，运行时 SO 文件名包含安装包 lastUpdateTime，避免覆盖安装继续使用旧壳或旧 AES 参数。启动先验证有独立 RSA 签名的完整性记录；缓存 SO 每次按记录中的 SHA-256 核对，损坏时重新提取。`dealJiagu:processResources` 和 `加固打包` 自动依赖 `zipJiaguLibrary`；重新编译桌面程序/加固 JAR 后再加固 APK，不能继续使用旧工具包。加固侧缓存键包含壳源码 ZIP 的 MD5 和完整性公钥，会随资源或公钥变更失效。

壳 App 的 ABI 过滤包含 x86/x86_64。加固工具提取、校验并缓存各 ABI 的 SO。所有加固 APK 都携带 ARMv7/arm64/x86/x86_64 壳资产；有 ARM 原生库的 APK 也可能由 Intel ART 通过 native bridge 运行，不能按原 APK 的 lib 目录过滤掉 Intel 壳资产。加载端读取 /proc/self/exe 的 ELF 架构选择壳，读取失败时回退到进程 os.arch。SO 先写临时文件并校验 ELF，再原子重命名；提取失败会保留 ABI、资产路径及原始异常。

## 验证

### API 21–25 与 ARMv7

Android 5.0 的 DefineClass 没有 Thread/hash 参数，采用单独回调；5.1 起使用带 Thread/hash 的回调。API 21/22 的 OpenMemory 返回裸 DexFile 指针，API 23–25 返回 unique_ptr。所有入口在调用前精确解析，缺失时失败，不调用空指针。32/64 位分别使用 size_t 的 j/m 符号及 lib/lib64 路径。

API 21/22 的 mCookie 保存原生 DexFile 指针 vector；API 23 保存 `[DexFile*]` long[]；API 24/25 保存 `[null OatFile*, DexFile*]` long[]，同时设置 mInternalCookie。修复了旧分支分配新数组却写回旧数组、错误下标及遗漏 mInternalCookie 的问题。每个业务 DEX 的 OpenMemory location 唯一；解密载荷与 DexFile 在进程内持续保留。为初始化旧版 Java DexFile，仅临时写入固定的 400 字节占位 DEX，加载后删除占位文件；业务 DEX 不写磁盘，方法仍在类定义前还原。

载荷中各 DEX 之间不保证对齐，不能直接把第二个 DEX 的载荷地址传给旧 ART。每个 DEX 都复制到独立页对齐的 mmap 后再注册和加载，避免 Android 5.0 的 IsAligned<4>(base) 断言；这些映射保留到进程结束。

ARMv7 Hook 支持 ARM/Thumb 状态与字面量指令重定位；修正了 Thumb-2 LDR literal 对 Align(PC,4) 的要求，并刷新被改写指令的缓存。`arm_hook_test` 覆盖 ARM/Thumb、调用原跳板、PC 相对字面量，以及 ART 入口使用的未四字节对齐 Thumb-2 指令布局。通过 NDK CMake 的 METHOD_RESTORE_TESTS 开关构建，需在 ARMv7 环境执行。

壳工程自身 minSdk 为 21；加固仍只替换原 Manifest 的 Application，不提升原应用声明的 minSdk/targetSdk。原应用低于 21 时，声明虽保留，壳的运行检查会拒绝低于 API 21 的系统；不应向这些设备分发加固包。

2026-11-10 最终验证：四种 ABI 的壳编译成功，主机六项回归和桌面调用层编译通过。同一 SHA-256 的独立双 DEX 混合加固包在以下设备各通过五项回归：

| API | ART 进程 ABI | 结果 |
|---|---|---|
| 21 | armeabi-v7a | 5/5 |
| 21 | x86 | 5/5 |
| 25 | x86 | 5/5 |
| 36 | x86_64 | 5/5 |

覆盖首次启动、SO 缓存复用、缓存损坏恢复、远程进程 Provider、同证书覆盖安装。Application/Provider/Activity 都校验 D2C、VMP、第三代方法还原和跨 DEX 调用。测试包均已卸载；证据保存在根目录 build/compatibility/device-emulator-5572、5570、5574、5554 的 results.json/evidence.log。ARM/Thumb Hook、方法记录解析及原生 AES 测试在 API 21 ARMv7 环境执行通过。上述结果不代表 API 22/23/24、旧版 arm64 或所有 OEM ART 均已实测，也不是 Keep 业务 APK 的验收结果。

`src/test/cpp/method_restore_test.cpp` 覆盖 direct/virtual/native 分类、按类恢复、源缓冲区复制、多 DEX 相同偏移、空记录、截断记录、越界偏移、长度不匹配、重复记录和损坏 ULEB128。

以 NDK CMake 配置 `-DMETHOD_RESTORE_TESTS=ON`，构建 `method_restore_test`；Intel ABI 还会构建 `x86_hook_test`。Android 构建的测试程序须推送对应 ABI 设备后执行。Linux 主机可用 GCC/G++ 编译 HDE、x86_hook.cpp 和 x86_hook_test.cpp；加 `-m32` 验证 32 位，默认验证 64 位。

2026-11-09：Linux 主机实际执行 x86 和 x86_64 Hook 测试通过，覆盖入口替换、调用原函数、相对 CALL、条件分支、内部跳转、x86 PIC、x64 RIP 寻址（含立即数）和失败时不修改入口；方法记录解析测试执行通过。这些不等于 Android ART 集成验证。最终还需在对应 ABI 的设备/模拟器用实际加固 APK 验证启动、Application/ContentProvider、跨 DEX 调用、延迟类加载和并发类加载。

## AES 参数一致性

DEX 前 512 字节与壳 SO 必须使用本次加固传入的同一组 AES-128 密钥和 IV。`JiaGu.deal` 显式向 `AES128Helper.encrypt` 传参，不依赖 UI 或全局变量是否同步。入口校验两者各为 16 个 UTF-8 字节；生成 C 源码时按字节转义，缓存日志不输出参数原文。原生解密校验长度及每个 PKCS#7 填充字节。

运行 `:dealJiagu:verifyAesPayload` 验证自定义参数与全局默认值不同的场景；原生 `aes_payload_test` 验证二进制载荷和损坏填充。2026-11-09 实际 APK 修复验证：用匹配壳的参数调用原生解密及方法记录解析，4 个 DEX、13 条抽离记录通过。此项仍不代替模拟器上的完整 ART 启动验证。

## Android 17 启动修复及验证

ART 隐藏 DefineClass 符号时，先读取同一库 ELF 的符号表，再用内置 XZ Embedded 解压 `.gnu_debugdata` 查找精确名称。只接受 FUNC 符号，并在已加载库的可执行 PT_LOAD 范围内换算地址；解析与解压有边界及 64 MiB 输出上限。XZ 来源版本和 0BSD 许可证保存在 `src/main/cpp/utils/xz/`。不依赖系统私有 liblzma 接口。

兼容旧 DefineClass 签名和 Android 17 新增一个 size_t 参数的签名，使用独立回调原样传递两个参数。未知签名或没有可用符号仍明确失败。方法抽离后打包端重算 DEX SHA1/Adler32；运行时为旧载荷重算 Adler32，避免 ART 在 DefineClass 前因 checksum 不符拒绝全部内存 DEX。内存加载的 suppressed IOException 会作为实际异常抛出。

业务 DEX 注入成功后才实例化原 Application；原 attach 失败会向外抛出。CoreComponentFactory 的各类组件使用捕获的业务加载器，原 attach 改变 dexElements 时补回缺少的元素。移除伪造包名及 createPackageContext 的行为。

2026-11-09：隔离的 Android 17/API 37 x86_64 模拟器实测 `Keep_jiagu_provider_fixed_unsigned.apk` 的同内容测试签名副本：安装成功，业务 DEX 注入成功，日志确认按类恢复抽离方法，原 Application 初始化完成，进入 MainActivity/KeepActivity 和通知权限弹窗，未再出现 VPR ClassNotFoundException。四种 ABI 原生库均构建通过；x86/arm64 的 Android 集成启动尚未验证。模拟器日志另有原应用原生保护子进程经 ARM native bridge 的 SIGSEGV；原版 APK 也记录到同类转译/JNI 崩溃，因此不能据此宣称应用所有原生子进程兼容模拟器。

`src/test/cpp/art_symbol_test.cpp` 以实际 API 37 libart.so 为输入，验证 XZ 解压、新签名精确匹配、非精确名称拒绝、截断 ELF 和越界 section 表；Linux 主机执行通过。修复包的原生解密验证仍为 4 个 DEX/13 条抽离记录，通过 16 KiB ZIP 对齐检查。

资源保护：主 APK 的资源表、res 和 assets 已纳入启动前内容签名校验，并提供可选资源文件路径混淆和轻量内容加密；运行时恢复资源后再加载原 Application。详见 [资源加固说明](../dealJiagu/RESOURCES.md)。
