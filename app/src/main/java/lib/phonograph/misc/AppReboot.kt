/*
 * Copyright (c) 2022~2023 chr_56
 */

package lib.phonograph.misc

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Process


object Reboot {

    private const val REBOOT_PROCESS_NAME_SUFFIX = "reboot"

    const val RESTART_INTENT = "restart_intent"
    const val PROCESS_PID_TO_KILL = "phoenix_main_process_pid"

    /**
     * trigger reboot
     */
    fun reboot(
        context: Context,
        pid: Int = Process.myPid(),
        restartIntent: Intent = restartIntent(context),
    ) {
        context.startActivity(
            Intent(context, RebootActivity::class.java)
                .apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(PROCESS_PID_TO_KILL, pid)
                    putExtra(RESTART_INTENT, restartIntent.also {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    })
                }
        )
    }

    class RebootActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Kill original process
            val pidToKill = intent.getIntExtra(PROCESS_PID_TO_KILL, -1)
            require(pidToKill > 0)
            Process.killProcess(pidToKill)

            // Start new one
            startActivity(
                @Suppress("DEPRECATION")
                when {
                    SDK_INT >= TIRAMISU -> intent.getParcelableExtra(RESTART_INTENT, Intent::class.java)
                    else                -> intent.getParcelableExtra(RESTART_INTENT) as? Intent
                }
            )


            // finish self
            finish()
            Runtime.getRuntime().exit(0)
        }
    }


    fun isRebootingProcess(context: Context): Boolean {

        val manager = context.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager

        val runningProcesses = manager.runningAppProcesses ?: return false
        val processInfo = runningProcesses.first { it.pid == Process.myPid() }
        if (processInfo.processName.endsWith(REBOOT_PROCESS_NAME_SUFFIX)) {
            return true
        }
        return false
    }

    /**
     * @return Launch Intent
     */
    fun restartIntent(context: Context): Intent {
        val launchingIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launchingIntent != null) {
            return launchingIntent
        } else {
            throw IllegalStateException("No LaunchIntent found")
        }
    }

}

