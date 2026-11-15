# Galaxy S24 (e1q / SM-S9210) DZJ3 临时 Root

这是 [Reallyliked21/galaxy-s24u-dzj3-temp-root](https://github.com/Reallyliked21/galaxy-s24u-dzj3-temp-root)
`v3.1-source` 源码包的一个分支版本，目的只有一个：**让它在 Galaxy S24 标准版（SM-S9210 / e1q）上真正跑起来**。

原仓库只认 `SM-S9280（e3q）` + `DZJ3`，且源码包本身存在两个致命问题（详见 [改动说明](#改动说明)）。
本仓库把这两个问题都修了，并在真机上完成了从构建到 root 的完整验证。

---

## 验证环境

| 项 | 值 |
|---|---|
| 机型 | Samsung Galaxy S24（SM-S9210，`device=e1q`，`product=e1qzcx`） |
| 系统 | Android 16 / One UI 8.5，编译编号 `BP4A.251205.006.S9210ZCS6DZJ3` |
| 内核 | `6.1.145-android14-11-3254743-abS9210ZCS6DZJ3` |
| 基带（CP） | `S9210ZCS6DZJ1`（OTA 未同步更新，与 root 无关） |
| 安全补丁 | 2026-08-05 |
| 平台 | `pineapple`（sm8650） |

> 关键点：S9280ZCS6DZJ3 与 S9210ZCS6DZJ3 **共用同一份内核镜像**
> （内核串 `6.1.145-android14-11-3254743` 完全一致，changelist 3254743，编译时间同为 2026-11-06）。
> 因此 `native/offsets.h` 中的 59 个内核偏移常量**一个都不需要改**。

## 实测结果

```
uid=0(root) gid=0(root) groups=0(root) context=u:r:ksu:s0
kernelsu 212992 2 - Live 0x0000000000000000 (O)
```

- KernelSU 用户态 `ksud`：**3.2.5-JamXisu**（versionCode `32525`），KMI `android14-6.1`
- 模块形态：LKM，通过 `ksud late-load` 挂载
- `ksud soft-reboot` 之后 LSPosed 正常加载
- 重启后失效（临时 root），需重新执行

## 改动说明

### 1. `native/build.sh`：`-shared` → `-pie -fPIE`（最关键）

原脚本用 `-shared` 链接两个 payload，产出的是**共享库**：

```
e_type = ET_DYN,  e_entry = 0x0,  无 PT_INTERP
```

而 `RootService` 是 `ProcessBuilder(timerBin.absolutePath, "umhpipe")` 直接 exec 它，
进程一跳进入口 0 就 `SIGSEGV`（`rc=139`），标准输出一条都没有。App 侧只是在
300 秒里轮询 `/proc/modules`，最后报「结束码=4」。

改成 PIE 后入口由 `crt1` 正常调用 `main()`：

```
ELF 64-bit LSB pie executable, ARM aarch64, interpreter /system/bin/linker64
```

### 2. 机型闸门：按内核构建号放行

`MainActivity.kt` / `RootService.kt` 里的判定：

```kotlin
// 原：只认 e3q + SM-S9280 + INCREMENTAL 含 DZJ3
if (Build.DEVICE != "e3q" || !Build.MODEL.contains("S9280") || target == null) …

// 改：按内核构建号 + e1q/e2q/e3q
private const val KERNEL_BUILD = "6.1.145-android14-11-3254743"
private val SUPPORTED_DEVICES = setOf("e3q", "e1q", "e2q")
```

`System.getProperty("os.version")` 拿到的就是 `uname -r`，比 `INCREMENTAL` 可靠。

### 3. 补齐运行时资产

源码包里**不含** `ksud-s25u-kdp`、`uhexec`、`umh_action.sh`，staging 会直接
`failed to stage embedded files`（结束码=1）。本仓库已把它们放进 `app/src/main/assets/`：

| 资产 | 来源 | 校验 |
|---|---|---|
| `ksud-s25u-kdp` | [BuSung-dev/Root-My-Galaxy-Payloads](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads) 的 `kernelsu/ksud-e3q-S9280ZCS6DZF2-kdp` | SHA-256 `1b92611de62035263f129636270deb9002de42e1d2329460342856243975015b`（与其 README 一致） |
| `uhexec` | [NanoTurtle1145/root-my-s24](https://github.com/NanoTurtle1145/root-my-s24) `app/src/main/assets/samtimer/uhexec` | SHA-256 `0a9db96c…c5be0` |
| `umh_action.sh` | 同上 | SHA-256 `32575e1e…2e99` |

### 4. `timer-dzj3-apk.c`：umh publish 竞态重试

`system_unbound_wq` 上经常有短命后台 work（tty hangup 之类）排队。原代码只采样一次：

```c
if (old_prev != worklist)
    park_forever("umh worklist busy before publish");
```

真机上 3 秒内跑完前面所有阶段、SELinux 已写成 permissive，然后**几乎必撞**这一下，
只能驻留等死。改成轮询等空窗：

```c
/* 每次最多 1000 次 × 200 µs（约 200 ms），最多 3 轮；
   next 校验失败则回滚 prev，再来一次 */
```

改完第一次就打通了。补丁见 `patches/umh-publish-retry.diff`。

## 构建

```bash
export ANDROID_NDK_HOME=$HOME/Library/Android/sdk/ndk/28.2.13676358
(cd native && ./build.sh)                 # 生成 PIE 可执行 payload
cp native/libtimer_std.so native/libtimer_lspd.so app/src/main/jniLibs/arm64-v8a/
./gradlew :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`（debug 签名，需自行重签或用 `adb install -r` 覆盖同名包）。

## 使用

1. `adb install -r app-debug.apk`
2. 打开 App，确认已连上 Shizuku（shell uid 2000）
3. 点「开始」，等内核模块出现（通常 10–60 秒）
4. 若要 LSPosed，点「手动软重启」

## 风险

- 这是 CVE-2026-98260 的利用代码，**只在你自己的设备上使用**；
- 失败可能触发内核 panic，重启即可恢复，但请自行承担风险；
- 临时 root，重启即失效；过程中不要强杀 App 进程（源码会主动驻留以保住 zombie 链）。

## 上游与致谢

- 原仓库：[Reallyliked21/galaxy-s24u-dzj3-temp-root](https://github.com/Reallyliked21/galaxy-s24u-dzj3-temp-root)（`v3.1-source`）
- 漏洞研究：Hyunwoo Kim (@v4bel) / V4bel/InfinityCore — CVE-2026-98260 `posix-cpu-timers` UAF
- 利用引擎：yoro1836/SamTimer — `timer.c` / `offsets.h`（MIT）
- UMH 流程 / Shizuku UserService：RiosWesley/SamTimer（MIT）
- KernelSU：KernelSU / JamXisu fork
- LSPosed：LSPosed 团队

原始许可证见 `LICENSE`、`LICENSES/`、`NOTICE`、`THIRD_PARTY_NOTICES.md`，本分支沿用上游授权。
