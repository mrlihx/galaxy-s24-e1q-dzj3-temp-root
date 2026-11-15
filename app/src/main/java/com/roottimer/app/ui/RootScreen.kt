package com.roottimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dopamine-inspired single-page UI:
 *  - big title + CVE subtitle
 *  - status card (root state / shizuku / module mode)
 *  - oversized pill action button
 *  - auto-root card
 *  - manager / battery shortcuts
 *  - log card
 */
@Composable
fun RootScreen(
    rootLabel: String,
    rootDetail: String,
    rootDot: Color,
    shizukuLabel: String,
    shizukuDot: Color,
    statusText: String,
    statusColor: Color,
    runLabel: String,
    runEnabled: Boolean,
    isRunning: Boolean,
    autoRoot: Boolean,
    moduleMode: Boolean,
    autoSoftReboot: Boolean,
    targetLabel: String,
    logs: List<String>,
    onRun: () -> Unit,
    onAutoToggle: (Boolean) -> Unit,
    onModuleToggle: (Boolean) -> Unit,
    onAutoSoftRebootToggle: (Boolean) -> Unit,
    onSoftRebootNow: () -> Unit,
    onFixKernelFeatures: () -> Unit,
    onExportLogs: () -> Unit,
    onOpenManager: () -> Unit,
    onOpenLsposed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        // ---------- header ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Galaxy S24U DZJ3",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "CVE-2026-98260 · 临时 Root",
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AccentSoft,
            ) {
                Text(
                    "DZJ3",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Accent,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---------- status card ----------
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = CardBg,
            shadowElevation = 10.dp,
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(rootDot.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (rootDot == Accent) "✓" else "●",
                            color = rootDot,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            rootLabel,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Text(
                            rootDetail,
                            fontSize = 12.sp,
                            color = TextSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(shizukuDot),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Shizuku", fontSize = 14.sp, color = TextSecondary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        shizukuLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (shizukuDot == Accent) Accent else Warn,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "目标固件：$targetLabel",
                    fontSize = 11.sp,
                    color = TextSecondary,
                )

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "加载 LSPosed / 模块",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Text(
                            if (moduleMode) "模块模式：会跑 module stage 并自动软重启，加载 LSPosed"
                            else "标准模式：最快最稳，不加载模块",
                            fontSize = 11.sp,
                            color = TextSecondary,
                        )
                    }
                    Switch(
                        checked = moduleMode,
                        onCheckedChange = onModuleToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Accent,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD0E0),
                        ),
                    )
                }

                if (statusText.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        statusText,
                        fontSize = 12.sp,
                        color = statusColor,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---------- big action button ----------
        val enabled = runEnabled && !isRunning
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    if (enabled) Brush.horizontalGradient(listOf(Accent, Accent2))
                    else Brush.horizontalGradient(listOf(Color(0xFFCBD0E0), Color(0xFFCBD0E0)))
                )
                .clickable(enabled = enabled) { onRun() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (isRunning) "执行中…（最多 7 分钟）" else runLabel,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(18.dp))

        // ---------- auto root ----------
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = CardBg,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "开机后自动 Root",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "等 Shizuku 上线后自动执行 · 失败自动停用",
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                }
                Switch(
                    checked = autoRoot,
                    onCheckedChange = onAutoToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCBD0E0),
                    ),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---------- auto soft reboot ----------
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = CardBg,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Root 后自动软重启",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "激活 LSPosed / 模块；关闭后需在 KernelSU 管理器里手动软重启",
                        fontSize = 11.sp,
                        color = TextSecondary,
                    )
                }
                Switch(
                    checked = autoSoftReboot,
                    onCheckedChange = onAutoSoftRebootToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCBD0E0),
                    ),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Shortcut("手动软重启（激活 LSPosed）", Modifier.fillMaxWidth(), onSoftRebootNow)

        Spacer(Modifier.height(10.dp))
        Shortcut("修复 KernelSU 特性（含 SELinux Hide）", Modifier.fillMaxWidth(), onFixKernelFeatures)

        Spacer(Modifier.height(14.dp))

        // ---------- shortcuts ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Shortcut("KernelSU 管理器", Modifier.weight(1f), onOpenManager)
            Shortcut("LSPosed", Modifier.weight(1f), onOpenLsposed)
            Shortcut("导出日志", Modifier.weight(1f), onExportLogs)
        }

        Spacer(Modifier.height(18.dp))

        // ---------- logs ----------
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = CardBg,
            shadowElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("日志", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("${logs.size} 行", fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (logs.isEmpty()) {
                        Text("暂无日志", fontSize = 11.sp, color = TextSecondary)
                    } else {
                        logs.takeLast(80).forEach { line ->
                            Text(
                                line.replace("\r", ""),
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (line.contains("error", true) || line.contains("fail", true))
                                    Danger else Color(0xFF4B5468),
                            )
                        }
                    }
                }
            }
        }

        // ---------- credits / open source ----------
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = CardBg,
            shadowElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("鸣谢 / 开源", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(3.dp))
                Text("本 App 站在以下研究/开源项目之上：", fontSize = 10.sp, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                CreditLine("漏洞研究", "Hyunwoo Kim (@v4bel) · V4bel/InfinityCore — CVE-2026-98260 posix-cpu-timers UAF")
                CreditLine("利用引擎", "yoro1836/SamTimer — timer.c / offsets.h（MIT）")
                CreditLine("APK / UMH 流程", "RiosWesley/SamTimer — Shizuku UserService、KDP-safe UMH（MIT）")
                CreditLine("98260 集成 / KSU", "NanoTurtle1145/root-my-s24 — 98260 引擎与 KernelSU late-load（GPL-3.0）")
                CreditLine("KDP ksud / 流程", "BuSung-dev/Root-My-Galaxy-Payloads — e3q KDP ksud 资产")
                CreditLine("KernelSU", "tiann/KernelSU（GPL-3.0）")
                CreditLine("Shizuku", "RikkaApps/Shizuku（Apache-2.0）")
                CreditLine("DZJ3 适配 / App", "本项目（Galaxy S24U DZJ3）——仅基于上述开源，未获原作者官方背书")
                Spacer(Modifier.height(8.dp))
                Text(
                    "以上项目版权归各自作者所有；本 App 仅用于自有设备的安全研究，不提供任何担保。",
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "重启后自动恢复 · 不写 boot / 分区",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Shortcut(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AccentSoft)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Accent,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CreditLine(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Accent)
        Text(value, fontSize = 10.sp, lineHeight = 13.sp, color = Color(0xFF4B5468))
    }
}
