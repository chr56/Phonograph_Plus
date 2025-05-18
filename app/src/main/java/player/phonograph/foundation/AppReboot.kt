/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation

import androidx.core.content.IntentCompat
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process


object Reboot {

    private const val REBOOT_PROCESS_NAME_SUFFIX = "reboot"
    private const val REBOOT_DEFAULT_ACTION = "reboot"

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
            Intent(context, RebootActivity::class.java).apply {
                action = REBOOT_DEFAULT_ACTION
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(PROCESS_PID_TO_KILL, pid)
                putExtra(RESTART_INTENT,
                    restartIntent.also { restartIntent ->
                        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                )
            }
        )
    }

    class RebootActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Kill original process
            val pidToKill = intent.getIntExtra(PROCESS_PID_TO_KILL, -1)
            if (pidToKill > 0) {
                Process.killProcess(pidToKill)
            }

            // Start new one
            startActivity(
                IntentCompat.getParcelableExtra<Intent>(intent, RESTART_INTENT, Intent::class.java)
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
        return processInfo.processName.endsWith(REBOOT_PROCESS_NAME_SUFFIX)
    }

    /**
     * @return Launch Intent
     */
    fun restartIntent(context: Context): Intent {
        val launchingIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: throw IllegalStateException("No LaunchIntent found")
        return launchingIntent
    }

}

