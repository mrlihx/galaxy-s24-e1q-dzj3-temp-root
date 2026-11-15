# Galaxy S24U DZJ3 临时 Root — v3.1 源码发布说明

## 这是什么

- 仅支持 **SM-S9280 国行（CHC）/ e3q / DZJ3**
- 固件：`S9280ZCS6DZJ3`
- 内核：`6.1.145-android14-11-3254743-abS9280ZCS6DZJ3`
- 漏洞：CVE-2026-98260（6.1.191 已修复；DZJ3 未修复）
- 方式：临时 root，不写 boot/init_boot、不修改分区、不熔断 KNOX；真实重启后消失

## 这个 release 包含

- `native/`：timer.c payload + DZJ3 offsets.h + NDK 编译脚本
- `app/`：Android App 源码（Kotlin/Compose + AIDL）
- `tools/`：DZJ3 定标脚本
- `docs/DZJ3_CALIBRATION.md`：偏移/调用点/BTF 校验记录
- `LICENSES/` + `THIRD_PARTY_NOTICES.md`

## 不包含

- `ksud` KDP 二进制（本源码 release 不含；来源 RMG Payloads，Apache-2.0，见 THIRD_PARTY_NOTICES）
- `uhexec`
- 预编译 `libtimer_*.so`
- APK / keystore / 签名产物

## 构建

见 [README.md](README.md) 的 "构建" 部分。

## 已验证

- SM-S9280 DZJ3 真机：临时 root 成功；
- KernelSU LKM live；
- LSPosed / Zygisk 加载；
- `ksud soft-reboot` 后 root 保留；
- App 固件校验（仅 SM-S9280 + e3q + DZJ3）与日志导出。

## 未验证

- 其他机型/固件（DZJ1 已移除，不提供）；
- 公开 APK 分发（等待 RMG #462 资产级确认 + release 签名）。

## 许可与鸣谢

整体 GPL-3.0；SamTimer MIT、Shizuku Apache-2.0、RMG Payloads Apache-2.0 等上游声明见
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

`ksud` 资产：`ksud-e3q-S9280ZCS6DZF2-kdp`，4,879,856 B，SHA-256 `1b92611de62035263f129636270deb9002de42e1d2329460342856243975015b`，来自 https://github.com/BuSung-dev/Root-My-Galaxy-Payloads 。
