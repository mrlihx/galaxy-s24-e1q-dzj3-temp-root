# 开源组件与鸣谢

本 App（Galaxy S24U DZJ3）基于以下研究与开源项目。版权归各自作者所有。

## 漏洞

- **CVE-2026-98260** — `exec: Cleanup POSIX timers right after de_thread()`
  - 报告/研究：**Hyunwoo Kim (@v4bel)** · [V4bel/InfinityCore](https://github.com/V4bel/InfinityCore)
  - 上游修复：https://github.com/torvalds/linux/commit/acb03d3881818581052924a9bbbe92b8741ed448

## 利用链 / 引擎

- **yoro1836/SamTimer** — https://github.com/yoro1836/SamTimer
  - `app/src/main/cpp/timer.c`、`offsets.h` 的来源；本 App 的 DZJ3 定标基于它。
  - License: MIT（以原仓库为准）。
- **RiosWesley/SamTimer** — https://github.com/RiosWesley/SamTimer
  - APK 壳、Shizuku UserService、KDP-safe UMH / bind-mount late-load 流程参考。
  - License: MIT（以原仓库为准）。

## 98260 集成 / KernelSU

- **NanoTurtle1145/root-my-s24** — https://github.com/NanoTurtle1145/root-my-s24
  - 98260 引擎集成、KDP ksud 资产、late-load 方案参考。
  - License: GPL-3.0（以原仓库为准）。
- **BuSung-dev/Root-My-Galaxy-Payloads** — https://github.com/BuSung-dev/Root-My-Galaxy-Payloads
  - e3q / 6.1.145 KDP `ksud` 资产与 Samsung KDP late-load 流程参考。
- **tiann/KernelSU** — https://github.com/tiann/KernelSU
  - KernelSU 管理器 / ksud / LKM。
  - License: GPL-3.0。

## 其他

- **RikkaApps/Shizuku** — https://github.com/RikkaApps/Shizuku
  - shell uid UserService 执行框架。
  - License: Apache-2.0。
- AndroidX / Jetpack Compose / WorkManager（Apache-2.0，Google）。

## 免责声明

本 App 仅用于自有设备或已获明确授权的安全研究。上述项目作者未对本 App 进行官方背书；使用风险自负。
