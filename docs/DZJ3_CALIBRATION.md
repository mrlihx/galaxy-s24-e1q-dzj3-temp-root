# DZJ3 定标记录

目标设备：SM-S9280 国行（e3q）
固件：`S9280ZCS6DZJ3`
内核：`6.1.145-android14-11-3254743-abS9280ZCS6DZJ3`
内核 Image SHA-256：`ab5fff43ac0018a1f74abf7f03eea4b5ce134c4eafbae530eca4c541e3385a99`
CVE：CVE-2026-98260（6.1.191 修复；6.1.145 仍受影响）

## 方法

1. 从 DZJ3 `Image` 用 vmlinux-to-elf 恢复符号（107,299 symbols，BTF 5.98 MB）；
2. 解析 `posix_cpu_timers` 路径：
   - `do_timer_create()` 中 `kmem_cache_alloc(posix_timers_cache, ...)` 的返回指令；
   - UAF 触发后重新分配同一 cache 的 `k_itimer`，与 pipe buffer 重叠；
3. 解析 cred/task 分配点：
   - `prepare_creds()` 中 `kmem_cache_alloc(cred_jar, ...)` 返回；
   - `dup_task_struct()` 中 `kmem_cache_alloc_node(task_struct_cachep, ...)` 返回；
4. 静态符号由 `.symtab` 直接读取，偏移相对 `KERNEL_PHYS_LOAD = 0xa8000000`；
5. 结构偏移用 BTF 校验（cred/task/k_itimer/pipe_buf_ops/workqueue/selinux_state）。

## 关键常量（offsets.h）

| 常量 | 值 |
|---|---|
| STATIC_TIMER_SITE / TIMER_SITE_LOW21 | `0xffffffc008193464` / `0x193464` |
| CRED_SITE_STATIC | `0xffffffc0080e509c` |
| TASK_SITE_STATIC / TASK_ALLOC_LOW21 | `0xffffffc0080ac204` / `0xac204` |
| SELINUX_STATE_STATIC | `0xffffffc00a521588` |
| PANIC_ON_WARN_STATIC | `0xffffffc00a23ae08` |
| SYSTEM_UNBOUND_WQ_STATIC | `0xffffffc00a23ae60` |
| UMH_EXEC_WORK_STATIC | `0xffffffc0080d39cc` |
| CREDS_FAST_HASH_STATIC | `0xffffffc00a5a5c90` |
| ROOT_USER_STATIC | `0xffffffc00a260c80` |
| INIT_USER_NS_STATIC | `0xffffffc00a2609f0` |
| INIT_UCOUNTS_STATIC | `0xffffffc00a262458` |
| INIT_GROUPS_STATIC | `0xffffffc00a261fa0` |
| ANON_PIPE_BUF_OPS_STATIC | `0xffffffc009219d90` |
| KERNEL_PHYS_LOAD | `0xa8000000` |
| WQ_DFL_PWQ_OFF | `0xb0`（BTF 校正，模板为 0x110） |

## 已验证

- `su -c id` → `uid=0(root)`，context `u:r:ksu:s0`；
- `kernelsu` LKM live；
- LSPosed / Zygisk companion 进程加载；
- `ksud soft-reboot` 后 uptime 连续、root 保留。

## DZJ1 备注

DZJ1（`S9280ZCS6DZJ1`，同为 6.1.145）的静态符号与 call site 与 DZJ3 相同，
但**未做真机验证**，本仓库 v3.1 不包含 DZJ1 目标。

## 注意

- 同一 boot 只能跑一次 UMH 链；失败/挂起需要重启；
- KernelSU 特性 `selinux_hide` 的运行时 `set` 在 KDP late-load 下会返回 `-95`，
  可用 `ksud feature load` 从配置重新应用；
- KDP ksud 二进制不在本仓库内。
