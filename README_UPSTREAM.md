# Galaxy S24U DZJ3 — 临时 Root（源码发布版，不含二进制/ksud）

仅支持 **SM-S9280 国行（CHC）/ e3q / DZJ3**：
`S9280ZCS6DZJ3`，内核 `6.1.145-android14-11-3254743-abS9280ZCS6DZJ3`。

利用 CVE-2026-98260（posix-cpu-timers UAF）获得临时 root：
不写 boot/init_boot、不修改分区、不熔断 KNOX；**真实重启后 root 消失**。

> ⚠️ 本项目用于自有设备的安全研究/测试。使用风险自负；不要用于未授权设备。

## 这个仓库为什么没有 APK / ksud？

- 本源码仓库不分发 `ksud` 二进制；其来源已定位为 `BuSung-dev/Root-My-Galaxy-Payloads` 的 `kernelsu/ksud-e3q-S9280ZCS6DZF2-kdp`（仓库 Apache-2.0，SHA-256 `1b92611de62035263f129636270deb9002de42e1d2329460342856243975015b`）；
- 预编译的 `libtimer_*.so`、`uhexec` 也不提交，改为从源码构建/自行获取；
- 因此这里只发布**源码、定标数据、构建脚本**。

## 目录

```
app/                 Android App 源码（Kotlin/Compose + AIDL）
native/              timer exploit 源码 + DZJ3 offsets.h + 编译脚本
tools/               DZJ3 内核定标脚本
docs/                DZJ3 定标记录
LICENSES/            GPL-3.0 / Apache-2.0 / 上游 LICENSE
THIRD_PARTY_NOTICES.md
PUBLISHING.md
```

## 构建

### 1. 编译 native payload

```bash
cd native
export ANDROID_NDK_HOME=/path/to/android-ndk-r29
./build.sh
```

得到 `libtimer_std.so` / `libtimer_lspd.so`，放入：

```
app/src/main/jniLibs/arm64-v8a/
```

### 2. 准备运行时资产（仓库不包含）

App 还需要以下文件，请从对应上游项目获取并放到 `app/src/main/assets/`：

- `ksud-s25u-kdp`：KDP patched `ksud`（RootMyS24 / RMG 资产）；
- `uhexec`：UMH helper；
- `umh_action.sh`：late-load 脚本（按你的 ksud 路径调整）。

这些文件不在本仓库分发；`ksud` 的来源/许可见 `THIRD_PARTY_NOTICES.md`。未来若随 APK 分发，需保留 Apache-2.0 NOTICE 并遵守 KernelSU GPL-3.0。

### 3. 构建 APK

需要 JDK 21、Android SDK 34、Gradle 8.13：

```bash
./gradlew :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`。

公开发布请使用 **release keystore** 签名，不要使用 debug key。

## App 功能

- 固件校验：只接受 `SM-S9280` + `e3q` + `DZJ3`；
- 标准 Root / LSPosed 模块模式；
- Root 后自动软重启（可关）与手动软重启按钮；
- KernelSU 特性重新应用（`ksud feature load`，恢复 SELinux Hide）；
- 日志导出（`下载/GalaxyS24U-DZJ3/`）。

## 鸣谢 / 许可

见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 和 [OPEN_SOURCE.md](OPEN_SOURCE.md)。
本仓库整体按 **GPL-3.0** 发布（见 [LICENSE](LICENSE)）；上游 MIT/Apache 组件保留其声明。
