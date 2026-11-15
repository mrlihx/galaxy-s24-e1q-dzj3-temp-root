# Third-party notices

本项目包含/参考了以下开源项目。源码发布遵循 GPL-3.0；各上游组件保留原许可证。

| 项目 | 用途 | 许可证 | 链接 |
|---|---|---|---|
| SamTimer (yoro1836) | POSIX timer UAF exploit 思路/代码 | MIT（按其仓库声明） | https://github.com/yoro1836/SamTimer |
| SamTimer (RiosWesley) | APK/UMH late-load 流程 | MIT（按其仓库声明） | https://github.com/RiosWesley/SamTimer |
| RootMyS24 (NanoTurtle1145) | 98260 集成、KDP/KernelSU late-load | GPL-3.0 | https://github.com/NanoTurtle1145/root-my-s24 |
| KernelSU (tiann) | LKM / ksud / manager 协议 | GPL-3.0 | https://github.com/tiann/KernelSU |
| Shizuku (RikkaApps) | 免 root shell 授权/UserService | Apache-2.0 | https://github.com/RikkaApps/Shizuku |
| BuSung-dev/Root-My-Galaxy-Payloads | `ksud-e3q-S9280ZCS6DZF2-kdp` 二进制（APK 运行时资产，源码仓库不分发） | Apache-2.0 | https://github.com/BuSung-dev/Root-My-Galaxy-Payloads |
| CVE-2026-98260 | 漏洞研究 | — | Hyunwoo Kim (@v4bel) / InfinityCore 等 |

## ksud 资产说明

- 文件：`kernelsu/ksud-e3q-S9280ZCS6DZF2-kdp`
- 来源仓库：https://github.com/BuSung-dev/Root-My-Galaxy-Payloads
- 仓库许可证：**Apache-2.0**
- 大小：4,879,856 B
- SHA-256：`1b92611de62035263f129636270deb9002de42e1d2329460342856243975015b`

本源码仓库不包含该二进制；若在 APK 中分发：
- 保留 Apache-2.0 LICENSE/NOTICE，并署名 BuSung-dev/RMG；
- ksud 为 KernelSU 衍生二进制，KernelSU 为 GPL-3.0，需同时提供 KernelSU 上游源码链接/来源说明；
- 资产级授权建议以 RMG issue 回复为准（issue #462）。

**不随本源码仓库分发**：
- `ksud` 二进制（见上）；
- `uhexec` 二进制；
- 预编译 `libtimer_*.so`。

如你是上述项目作者并希望调整署名/授权，请开 issue 联系。
