package com.roottimer.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Boots the auto-root chain when the device finishes booting (or when the
 * app is updated).  The chain is two chained WorkManager jobs:
 *
 *   ShizukuWaitWorker - waits for Shizuku's binder, up to 8 minutes per run;
 *                       when Shizuku is not up yet it returns retry() so the
 *                       manager keeps trying after a fresh boot instead of
 *                       giving up (the user may start Shizuku minutes later).
 *   AutoRootWorker    - binds the UserService and runs the exploit.
 *
 * A failed *run* still disables the toggle (boot-loop protection), but a
 * missing/not-yet-running Shizuku only causes retries.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(Prefs.AUTO_ROOT, false)) return

        val wait = OneTimeWorkRequestBuilder<ShizukuWaitWorker>()
            .setInitialDelay(20, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
            .build()
        val run = OneTimeWorkRequestBuilder<AutoRootWorker>()
            .setBackoffCriteria(BackoffPolicy.LINEAR, 2, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context)
            .beginUniqueWork("auto_root", ExistingWorkPolicy.REPLACE, wait)
            .then(run)
            .enqueue()
    }
}
