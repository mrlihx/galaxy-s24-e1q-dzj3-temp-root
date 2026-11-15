# 发布前须知（v3.1 / DZJ3 临时 Root）

## 1. 现在有没有别人适配 DZJ3？

截至 2026-11-13 的公开检索（GitHub / XDA / 网页 / 酷安关键词）**没有找到公开的 DZJ3 适配**：

- RootMyS24 v3.6.3：98260 引擎只覆盖 One UI 9.0 / **6.1.162**（EZIF/EZJ1 等）；6.1.145 只走 GhostLock，而 DZJ3 已修 43499；
- SamTimer / RiosWesley fork：S921N / S928B 的 EZIF/EZJ1；
- GitHub 搜 `S9280ZCS6DZJ3` 只有固件下载页；
- NanoTurtle 的 98260 研究仓库归档在 EZIF。

所以这个 DZJ3 方案目前看是**第一个公开的**（不排除酷安/QQ 群有私下版本）。可以发布，但先把下面的合规问题处理掉。

## 2. 能不能“不加鸣谢直接发布”？

**不行（对现在这个 APK 而言）。** “鸣谢”是礼貌，**许可证是法律义务**：

| 代码/资产 | 许可证 | 你必须做的事 |
|---|---|---|
| yoro1836/SamTimer 的 timer.c / offsets.h | MIT | 保留版权声明和 MIT 许可文本 |
| RiosWesley/SamTimer 的 APK 壳/UMH 流程 | MIT | 保留版权声明和 MIT 许可文本 |
| RootMyS24（98260 集成、KSU late-load） | GPL-3.0 | 提供对应源码、保留声明、衍生作品按 GPL-3.0 发布 |
| KernelSU | GPL-3.0 | 同上 |
| Shizuku | Apache-2.0 | 保留 LICENSE / NOTICE |
| `ksud-e3q-S9280ZCS6DZF2-kdp`（KDP ksud 二进制） | 来源 RMG Payloads 仓库 **Apache-2.0**（已核对 SHA-256） | 保留 Apache-2.0 LICENSE/NOTICE、署名 BuSung-dev/RMG；ksud 为 KernelSU 衍生，需同时满足 KernelSU GPL-3.0 的源码义务；建议等 RMG issue #462 确认资产级授权 |

RootMyS24 的 `FINAL_REPORT.md` 曾提示 ksud 资产来源不清；现已定位到 RMG Payloads 仓库（Apache-2.0），并在 RMG issue #462 请求资产级确认；在回复前仍建议只发源码、不公开 APK。

**只发布你自己原创的定标数据/脚本/文档**（不包含上面这些代码和二进制）时，法律上不强制你鸣谢；但 CVE 研究署名是社区惯例，建议保留。只要 APK 里还有上面的代码/ksud，就不能靠“删鸣谢”绕过许可证。

## 3. 怎么联系作者？

没有找到公开邮箱，最可靠的是 GitHub Issues / PR / XDA 帖子：

| 对象 | 联系入口 | 建议问什么 |
|---|---|---|
| NanoTurtle1145（RootMyS24 / 98260 研究） | [root-my-s24 Issues](https://github.com/NanoTurtle1145/root-my-s24/issues)（issue #38 已开）、GitHub @NanoTurtle1145、XDA RootMyS24 帖 | ① 能否把 DZJ3 target 合入官方 ② KDP ksud 资产能否授权再分发 |
| RiosWesley | [RiosWesley/SamTimer Issues](https://github.com/RiosWesley/SamTimer/issues) | APK 壳/UMH 流程的再分发/衍生发布确认 |
| yoro1836 | [yoro1836/SamTimer Issues](https://github.com/yoro1836/SamTimer/issues) | MIT 代码使用确认（一般不需要额外授权，但可告知） |
| BuSung-dev（RMG / Payloads） | [Root-My-Galaxy-Payloads Issues](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads/issues/462) | `ksud` 资产级授权 + KernelSU GPL 源码说明（已发 #462） |
| XDA 原帖作者 | XDA 私信/回帖（fntz1 等，见 SamTimer 帖） | 同上 |

### 建议的 issue 模板（可直接改）

```text
Hi, I've ported the CVE-2026-98260 chain to SM-S9280 CHC DZJ3 (6.1.145).
I have offline calibration data (offsets/call sites/BTF) and a working
test build for DZJ3 (temporary root + KernelSU + LSPosed).

Two questions:
1) Would you accept a PR adding the DZJ3 target / offsets to the project?
2) The KDP ksud asset (ksud-e3q-S9280ZCS6DZF2-kdp) appears to have no
   explicit license. May I redistribute it in a public build, or should
   users obtain it themselves?

I will keep full attribution and follow the project's license.
```

## 4. 推荐的发布路线

**A. 最稳：源码 + 定标数据，不打包 ksud**
- GitHub 仓库：`offsets.h`、timer.c 修改、DZJ3 校准报告、构建脚本、风险说明；
- 用户自己准备 KernelSU 3.2.5 管理器 / ksud；
- 没有 ksud 授权问题，也方便 review。

**B. 给上游提 PR**
- 按 RootMyS24 的 adaptation guide 提交 DZJ3 target；
- 作者合入后由官方发布，你不需要自己发 APK。

**C. 一定要发 APK**
- 保留 RMG Apache-2.0 NOTICE、KernelSU GPL-3.0 来源说明；建议等 RMG #462 回复确认；
- 用 **release keystore** 签名，不要用 debug；
- 只保留 **SM-S9280 CHC DZJ3** 校验（v3.1 已做）；
- 发布页写清楚风险/恢复方式/SHA-256/鸣谢；不要宣称官方或 KNOX 绝对安全。

## 5. v3.1 当前状态

- 只接受 `SM-S9280` + `e3q` + `DZJ3` 固件；
- 保留日志导出（`下载/GalaxyS24U-DZJ3/`）；
- 带鸣谢/开源页面；
- **仍是 debug 签名**，仅适合测试，公开前需要 release 签名和 ksud 资产级确认。
