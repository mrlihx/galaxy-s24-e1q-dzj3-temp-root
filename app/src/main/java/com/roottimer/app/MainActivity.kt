package com.roottimer.app

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import android.provider.MediaStore
import android.os.Environment
import android.content.ContentValues
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.roottimer.app.ui.Accent
import com.roottimer.app.ui.Danger
import com.roottimer.app.ui.RootScreen
import com.roottimer.app.ui.RootTimerColors
import com.roottimer.app.ui.TextPrimary
import com.roottimer.app.ui.TextSecondary
import com.roottimer.app.ui.Warn
import rikka.shizuku.Shizuku
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * S9280ZCS6DZJ3 (e3q) and S9210ZCS6DZJ3 (e1q) ship the *same* kernel image:
 * 6.1.145-android14-11-3254743.  Gate on the kernel build number instead of
 * MODEL / DEVICE so e1q/e2q are accepted as well.
 */
private const val DZJ3_KERNEL_BUILD = "6.1.145-android14-11-3254743"
private val DZJ3_SUPPORTED_DEVICES = setOf("e3q", "e1q", "e2q")

class MainActivity : ComponentActivity() {

    private val deviceTarget: String? =
        if (System.getProperty("os.version").orEmpty()
                .contains(DZJ3_KERNEL_BUILD)
        ) "DZJ3" else null
    private val deviceSupported: Boolean =
        DZJ3_SUPPORTED_DEVICES.contains(Build.DEVICE) && deviceTarget == "DZJ3"

    private val main = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)

    // ── Compose state ────────────────────────────────────────────────
    private var rootLabel by mutableStateOf("")
    private var rootDetail by mutableStateOf(" ")
    private var rootDot by mutableStateOf(Color(0xFF8B98A5))
    private var shizukuLabel by mutableStateOf("")
    private var shizukuDot by mutableStateOf(Color(0xFF8B98A5))
    private var runLabel by mutableStateOf("")
    private var runEnabled by mutableStateOf(false)
    private var isRunning by mutableStateOf(false)
    private var statusText by mutableStateOf("")
    private var statusColor by mutableStateOf(Color(0xFF8B98A5))
    private var autoRoot by mutableStateOf(false)
    private var moduleMode by mutableStateOf(false)
    private var autoSoftReboot by mutableStateOf(true)
    private var showRunConfirm by mutableStateOf(false)
    private var showAutoConfirm by mutableStateOf(false)
    private var showManagerDialog by mutableStateOf(false)
    private val logs = mutableStateListOf<String>()

    private var boundService: IRootService? = null
    private var boundArgs: Shizuku.UserServiceArgs? = null
    private var runRequested = false

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQ_PERM &&
                grantResult == PackageManager.PERMISSION_GRANTED
            ) {
                main.post {
                    appendLog(getString(R.string.log_granted))
                    ensureBound()
                }
            } else {
                main.post {
                    appendLog(getString(R.string.log_permission_denied))
                    setStatus(getString(R.string.status_permission_denied), Danger)
                    rootLabel = "无法检查"
                    rootDetail = "需要 Shizuku 权限"
                }
            }
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        main.post {
            shizukuLabel = getString(R.string.shizuku_connected)
            shizukuDot = Accent
            runEnabled = true
            // 권한이 이미 있으면 상태 조회용으로 서비스를 바인딩해 둔다
            if (checkPermission()) ensureBound() else runCatching {
                Shizuku.requestPermission(REQ_PERM)
            }
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        main.post {
            shizukuLabel = getString(R.string.shizuku_dead)
            shizukuDot = Danger
            setStatus(getString(R.string.status_shizuku_dead), Danger)
        }
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder?.pingBinder() != true) return
            val service = IRootService.Stub.asInterface(binder)
            boundService = service
            if (runRequested) {
                runRequested = false
                launch(service)
            } else {
                main.post { updateRootBadge() }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }
    }

    /** Bind the UserService for status queries (and reuse it for runs). */
    private fun ensureBound() {
        if (boundService != null || !checkPermission()) return
        val args = Shizuku.UserServiceArgs(
            ComponentName(packageName, RootService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("roottimer")
            .debuggable(false)
            .version(4)
        boundArgs = args
        runCatching { Shizuku.bindUserService(args, conn) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        autoRoot = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
            .getBoolean(Prefs.AUTO_ROOT, false)
        moduleMode = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
            .getBoolean(Prefs.MODE_LSPD, false)
        autoSoftReboot = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
            .getBoolean(Prefs.AUTO_SOFT_REBOOT, true)

        if (!deviceSupported) {
            rootLabel = "不支持的机型/固件"
            rootDetail = "仅支持 SM-S9280（e3q）DZJ3"
            rootDot = Danger
            runEnabled = false
            setStatus(
                "已阻止执行：model=${Build.MODEL} device=${Build.DEVICE} " +
                    "incremental=${Build.VERSION.INCREMENTAL}",
                Danger
            )
        } else {
            appendLog("目标固件：$deviceTarget（${Build.VERSION.INCREMENTAL}）")
        }
        rootLabel = getString(R.string.root_checking)
        shizukuLabel = getString(R.string.shizuku_checking)
        runLabel = getString(R.string.run_root)

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        if (Shizuku.pingBinder()) {
            shizukuLabel = getString(R.string.shizuku_connected)
            shizukuDot = Accent
            runEnabled = true
        } else {
            shizukuLabel = getString(R.string.shizuku_waiting)
            shizukuDot = Warn
            setStatus(getString(R.string.status_shizuku_start), Warn)
        }
        updateRootBadge()

        setContent {
            MaterialTheme(colorScheme = RootTimerColors) {
                RootScreen(
                    rootLabel = rootLabel,
                    rootDetail = rootDetail,
                    rootDot = rootDot,
                    shizukuLabel = shizukuLabel,
                    shizukuDot = shizukuDot,
                    statusText = statusText,
                    statusColor = statusColor,
                    runLabel = runLabel,
                    runEnabled = runEnabled,
                    isRunning = isRunning,
                    autoRoot = autoRoot,
                    moduleMode = moduleMode,
                    targetLabel = deviceTarget ?: "不支持",
                    autoSoftReboot = autoSoftReboot,
                    logs = logs,
                    onRun = { showRunConfirm = true },
                    onAutoToggle = { checked ->
                        if (checked) showAutoConfirm = true
                        else {
                            autoRoot = false
                            getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
                                .edit().putBoolean(Prefs.AUTO_ROOT, false).apply()
                            appendLog(getString(R.string.log_auto_off))
                        }
                    },
                    onModuleToggle = { checked ->
                        moduleMode = checked
                        getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
                            .edit().putBoolean(Prefs.MODE_LSPD, checked).apply()
                        appendLog(
                            if (checked) "已切换：LSPosed/模块模式（会跑 module stage，可能软重启）"
                            else "已切换：标准模式（最快最稳，不加载模块）"
                        )
                    },
                    onAutoSoftRebootToggle = { checked ->
                        autoSoftReboot = checked
                        getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
                            .edit().putBoolean(Prefs.AUTO_SOFT_REBOOT, checked).apply()
                        appendLog(if (checked) "已开启：Root 后自动软重启（激活 LSPosed）" else "已关闭：Root 后不自动软重启")
                    },
                    onSoftRebootNow = { softRebootNow() },
                    onFixKernelFeatures = { fixKernelFeatures() },
                    onExportLogs = { exportLogs() },
                    onOpenManager = { openManagerApp() },
                    onOpenLsposed = { openLsposed() },
                )

                if (showRunConfirm) {
                    val alreadyRooted = rootDot == Accent
                    AlertDialog(
                        onDismissRequest = { showRunConfirm = false },
                        title = {
                            Text(
                                stringResource(
                                    if (alreadyRooted) R.string.dialog_rerun_title
                                    else R.string.dialog_run_title
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                stringResource(
                                    if (alreadyRooted) R.string.dialog_rerun_msg
                                    else R.string.dialog_run_msg
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showRunConfirm = false
                                startRun()
                            }) { Text(stringResource(R.string.dialog_confirm_run), fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRunConfirm = false }) { Text(stringResource(R.string.dialog_cancel)) }
                        },
                    )
                }

                if (showAutoConfirm) {
                    AlertDialog(
                        onDismissRequest = {
                            showAutoConfirm = false
                            autoRoot = false
                        },
                        title = { Text(stringResource(R.string.dialog_auto_title), fontWeight = FontWeight.Bold) },
                        text = {
                            Text(stringResource(R.string.dialog_auto_msg))
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showAutoConfirm = false
                                autoRoot = true
                                getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
                                    .edit().putBoolean(Prefs.AUTO_ROOT, true).apply()
                                appendLog(getString(R.string.log_auto_on))
                                requestNotifPermissionIfNeeded()
                            }) { Text(stringResource(R.string.dialog_auto_confirm), fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showAutoConfirm = false
                                autoRoot = false
                            }) { Text(stringResource(R.string.dialog_cancel)) }
                        },
                    )
                }
                if (showManagerDialog) {
                    AlertDialog(
                        onDismissRequest = { showManagerDialog = false },
                        title = { Text(stringResource(R.string.manager_dialog_title), fontWeight = FontWeight.Bold) },
                        text = { Text(stringResource(R.string.manager_dialog_msg)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showManagerDialog = false
                                openManagerApp()
                            }) { Text(stringResource(R.string.manager_dialog_open), fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showManagerDialog = false }) {
                                Text(stringResource(R.string.manager_dialog_later))
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        main.post(badgePoller)
    }

    override fun onPause() {
        super.onPause()
        main.removeCallbacks(badgePoller)
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { Shizuku.removeBinderReceivedListener(binderReceivedListener) }
        runCatching { Shizuku.removeBinderDeadListener(binderDeadListener) }
        runCatching { Shizuku.removeRequestPermissionResultListener(permissionResultListener) }
        boundArgs?.let { runCatching { Shizuku.unbindUserService(it, conn, true) } }
    }

    private val badgePoller = object : Runnable {
        override fun run() {
            updateRootBadge()
            main.postDelayed(this, 3000)
        }
    }

    private fun updateRootBadge() {
        if (running.get()) return
        val rooted = try {
            File("/proc/modules").useLines { lines ->
                lines.any { it.startsWith("kernelsu ") }
            }
        } catch (_: Exception) {
            null
        } ?: try {
            boundService?.isRooted()
        } catch (_: Exception) {
            null
        } ?: run {
            rootLabel = getString(R.string.root_unknown)
            rootDetail = getString(R.string.root_unknown_detail)
            rootDot = Warn
            return
        }
        if (rooted) {
            rootLabel = getString(R.string.root_rooted)
            rootDetail = getString(R.string.root_rooted_detail)
            rootDot = Accent
            runLabel = getString(R.string.run_reroot)
        } else {
            rootLabel = getString(R.string.root_not_rooted)
            rootDetail = getString(R.string.root_not_detail)
            rootDot = TextSecondary
            runLabel = getString(R.string.run_root)
        }
    }

    private fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF
            )
        }
    }

    private fun startRun() {
        if (!deviceSupported) {
            appendLog("不支持的机型/固件，已阻止执行；仅支持 SM-S9280（e3q）DZJ1 / DZJ3")
            setStatus("不支持的机型/固件", Danger)
            return
        }
        if (!running.compareAndSet(false, true)) return
        isRunning = true
        runEnabled = false
        runLabel = getString(R.string.run_running)
        logs.clear()
        appendLog(getString(R.string.log_run_started))
        runRequested = true
        if (Shizuku.getVersion() >= 11 && !checkPermission()) {
            setStatus(getString(R.string.status_requesting), Warn)
            runCatching { Shizuku.requestPermission(REQ_PERM) }
                .onFailure {
                    runRequested = false
                    running.set(false)
                    isRunning = false
                    runEnabled = true
                    appendLog("error: ${it.message}")
                    setStatus(getString(R.string.status_request_failed), Danger)
                }
            return
        }
        val svc = boundService
        if (svc != null) {
            runRequested = false
            launch(svc)
        } else {
            ensureBound() // onServiceConnected launches it
        }
    }

    private fun checkPermission(): Boolean =
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    private fun launch(service: IRootService) {
        setStatus(getString(R.string.status_running), Warn)
        val cb = object : IRootCallback.Stub() {
            override fun onLog(line: String) {
                main.post { appendLog(line) }
            }

            override fun onComplete(exitCode: Int, ksuLoaded: Boolean) {
                main.post {
                    appendLog(getString(R.string.log_exit_fmt, exitCode, ksuLoaded))
                    if (ksuLoaded) {
                        appendLog(getString(R.string.log_root_ok))
                        setStatus(getString(R.string.status_root_ok), Accent)
                        // The "install KernelSU manager" dialog is only
                        // useful once; never nag on every successful run.
                        val mp = getSharedPreferences(Prefs.NAME, MODE_PRIVATE)
                        if (!mp.getBoolean(Prefs.MANAGER_DIALOG_SHOWN, false)) {
                            showManagerDialog = true
                            mp.edit().putBoolean(Prefs.MANAGER_DIALOG_SHOWN, true).apply()
                        }
                    } else if (exitCode == 0) {
                        appendLog(getString(R.string.log_unverified))
                        setStatus(getString(R.string.status_done_unverified), Warn)
                    } else {
                        appendLog(getString(R.string.log_failed))
                        setStatus(getString(R.string.status_failed_fmt, exitCode), Danger)
                    }
                    running.set(false)
                    isRunning = false
                    runEnabled = true
                    updateRootBadge()
                    // Keep the UserService bound: the badge poller uses it to
                    // read /proc/modules (the app uid cannot).  Unbound in
                    // onDestroy.
                }
            }
        }
        runCatching { service.setMode(moduleMode) }
        runCatching { service.setSoftReboot(autoSoftReboot) }
        runCatching { service.runExploit(cb) }
            .onFailure {
                main.post {
                    appendLog("error: ${it.message}")
                    setStatus(getString(R.string.status_error), Danger)
                    running.set(false)
                    isRunning = false
                    runEnabled = true
                    runLabel = "루트 실행"
                }
            }
    }

    private fun exportLogs() {
        Thread({
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val name = "GalaxyS24U-DZJ3-log-${deviceTarget ?: "unknown"}-$ts.txt"
            val sb = StringBuilder()
            sb.appendLine("Galaxy S24U DZJ3 诊断日志")
            sb.appendLine("time=$ts")
            sb.appendLine("app=" + runCatching {
                packageManager.getPackageInfo(packageName, 0).versionName
            }.getOrDefault("?"))
            sb.appendLine("target=${deviceTarget ?: "UNSUPPORTED"}")
            sb.appendLine("model=${Build.MODEL} device=${Build.DEVICE} brand=${Build.BRAND}")
            sb.appendLine("incremental=${Build.VERSION.INCREMENTAL}")
            sb.appendLine("fingerprint=${Build.FINGERPRINT}")
            sb.appendLine("kernel=${System.getProperty("os.version")}")
            sb.appendLine("mode=${if (moduleMode) "LSPosed" else "standard"} autoSoftReboot=$autoSoftReboot autoRoot=$autoRoot")
            sb.appendLine("shizuku=${shizukuLabel} root=${rootLabel}")
            sb.appendLine("--- logs ---")
            logs.forEach { sb.appendLine(it) }
            val text = sb.toString()
            var saved = false
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/GalaxyS24U-DZJ3"
                    )
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                    saved = true
                }
            }
            val where = if (saved) {
                "下载/GalaxyS24U-DZJ3/$name"
            } else {
                runCatching {
                    val f = java.io.File(getExternalFilesDir(null), name)
                    f.writeText(text)
                    f.absolutePath
                }.getOrElse { "保存失败：${it.message}" }
            }
            main.post {
                appendLog("日志已导出：$where")
                setStatus("日志已导出：$where", Accent)
            }
        }, "export-logs").start()
    }

    private fun appendLog(line: String) {
        if (logs.size > 4000) return // hard cap
        logs.add(line)
    }

    private fun fixKernelFeatures() {
        val svc = boundService
        if (svc == null) {
            appendLog("请先授予 Shizuku 权限/等待连接后再修复特性")
            ensureBound()
            return
        }
        appendLog("正在重新应用 KernelSU 特性配置…")
        val cb = object : IRootCallback.Stub() {
            override fun onLog(line: String) {
                main.post { appendLog(line) }
            }

            override fun onComplete(exitCode: Int, ksuLoaded: Boolean) {
                main.post {
                    appendLog("特性配置 rc=$exitCode")
                    if (exitCode == 0) setStatus("KernelSU 特性已重新应用（含 SELinux Hide）", Accent)
                    else setStatus("特性配置失败 rc=$exitCode（需要已 root）", Danger)
                    updateRootBadge()
                }
            }
        }
        runCatching { svc.fixKernelFeatures(cb) }
            .onFailure { appendLog("修复特性请求失败: ${it.message}") }
    }

    private fun softRebootNow() {
        val svc = boundService
        if (svc == null) {
            appendLog("请先授予 Shizuku 权限/等待连接后再软重启")
            ensureBound()
            return
        }
        appendLog("手动触发软重启…")
        runCatching { svc.softRebootNow() }
            .onFailure { appendLog("软重启请求失败: ${it.message}") }
    }

    private fun openManagerApp() {
        openAppOrUrl("me.weishu.kernelsu", getString(R.string.manager_url))
    }

    private fun openLsposed() {
        openAppOrUrl("org.lsposed.manager", null)
    }

    private fun openAppOrUrl(pkg: String, url: String?) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            runCatching { startActivity(launch) }
                .onFailure { appendLog("打开 $pkg 失败: ${it.message}") }
            return
        }
        if (url != null) {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                .onFailure { appendLog("打开链接失败: ${it.message}") }
        } else {
            appendLog("未安装 $pkg")
        }
    }

    private fun setStatus(text: String, color: Color) {
        statusText = text
        statusColor = color
    }

    companion object {
        private const val REQ_PERM = 101
        private const val REQ_NOTIF = 102
    }
}
