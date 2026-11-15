# Outreach / 作者沟通草稿

> 只使用公开渠道（GitHub Issues / PR / XDA 回帖），不要发垃圾信息。
> 发送前把 `<repo-url>`、`<你的名字>` 替换成实际内容。

## 1. RootMyS24（NanoTurtle1145）— 建议先评论在已有 issue #38

如果 #38 还在讨论 DZJ3，直接补一条评论；否则新开 issue。

```text
Hi NanoTurtle1145,

I've ported the CVE-2026-98260 chain to the Chinese S24 Ultra build
S9280ZCS6DZJ3 (6.1.145, e3q). The offsets/call sites are calibrated
and the exploit + KernelSU + LSPosed path has been tested on a real
SM-S9280 DZJ3 device.

Data I can share:
- do_timer_create / prepare_creds / dup_task_struct call-site offsets
- static symbol offsets (selinux_state, system_unbound_wq,
  call_usermodehelper_exec_work, creds_fast_hash, etc.)
- BTF-verified struct offsets
- a working app implementation (source available)

Two questions:
1) Would you accept a PR adding the DZJ3 target to root-my-s24?
2) The KDP ksud asset used for late-load (hash 1b92611d...,
   4,879,856 bytes) appears to have no explicit LICENSE. May I
   redistribute it in a public build, or should users obtain it
   from your project themselves?

I will keep full attribution and follow the project's license.
Source repo (will be public): <repo-url>
```

## 2. RMG / Root-My-Galaxy-Payloads（BuSung-dev）

```text
Hi BuSung-dev,

I'm working on a temporary root port for SM-S9280 CHC DZJ3
(6.1.145, e3q) and would like to clarify the license of the
KDP ksud asset:

  file: ksud-e3q-S9280ZCS6DZF2-kdp (4,879,856 bytes)
  sha256: 1b92611de62035263f129636270deb9002de42e1d2329460342856243975015b

The repository doesn't seem to contain an explicit LICENSE for this
binary. May I redistribute it (with attribution) in a public build,
or should users download it from your repo themselves?

If you prefer, I can also submit the DZJ3 target/offsets upstream
instead of publishing a separate binary.

Thanks!
```

## 3. RiosWesley / SamTimer（MIT 上游，礼貌通知）

```text
Hi RiosWesley,

I've built a DZJ3 (SM-S9280, 6.1.145, e3q) port based on the
SamTimer exploit/UMH flow. Your MIT-licensed work is credited in
THIRD_PARTY_NOTICES and the repo keeps the MIT notice.

I'm publishing source + offsets (no ksud binary) and will ask
NanoTurtle/RMG separately about the KDP ksud asset. Just letting
you know; no action needed if you're fine with the MIT terms.

Source repo: <repo-url>
```

## 4. yoro1836 / SamTimer（上游，可选）

一句话说明使用了 MIT 的 timer.c 思路，保留 MIT notice 即可；如果对方希望额外署名方式，按对方要求调整。

## 5. XDA 回帖（可选）

在 RootMyS24 的 XDA 帖里简短说明 DZJ3 端口 + 源码仓库链接；不要贴 APK，先按版规和作者意愿。
