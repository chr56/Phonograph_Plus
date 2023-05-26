package player.phonograph.ui.activities

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import lib.phonograph.activity.ToolbarActivity
import lib.phonograph.dialog.alertDialog
import lib.phonograph.misc.Reboot
import mt.util.color.primaryTextColor
import player.phonograph.R
import player.phonograph.databinding.ActivityCrashBinding
import player.phonograph.mechanism.SettingDataManager
import player.phonograph.notification.ErrorNotification.KEY_IS_A_CRASH
import player.phonograph.notification.ErrorNotification.KEY_STACK_TRACE
import player.phonograph.util.text.currentDateTime
import player.phonograph.util.text.getDeviceInfo
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import androidx.lifecycle.lifecycleScope
import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import android.view.Menu
import android.view.Menu.NONE
import android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import mt.color.R as MR

class CrashActivity : ToolbarActivity() {

    private lateinit var binding: ActivityCrashBinding

    private var crashReportMode = true

    private val colorPrimaryDeep get() =  resources.getColor(
        if (crashReportMode) MR.color.md_deep_orange_900 else MR.color.md_grey_800, theme
    )
    private val colorPrimary get() =  resources.getColor(
        if (crashReportMode) MR.color.md_deep_orange_700 else MR.color.md_grey_700, theme
    )


    private fun setupTheme() {
        // statusbar theme
        updateAllColors(colorPrimaryDeep)

        // toolbar theme
        binding.toolbar.apply {
            setBackgroundColor(colorPrimary)
            title = if (crashReportMode) getString(R.string.crash) else "Internal Error"
            setSupportActionBar(this)
        }
        // float button
        binding.copyToClipboard.apply {
            backgroundTintList = ColorStateList(
                arrayOf(
                    intArrayOf(-android.R.attr.state_pressed),
                    intArrayOf(android.R.attr.state_pressed)
                ),
                intArrayOf(
                    colorPrimary,
                    colorPrimaryDeep
                )
            )
            setColorFilter(primaryTextColor(colorPrimary))
        }

    }

    /**
     * stack trace text
     */
    private lateinit var stackTraceText: String

    /**
     * device data
     */
    private lateinit var deviceInfo: String

    /**
     *  full report
     */
    private lateinit var displayText: String

    override fun onCreate(savedInstanceState: Bundle?) {

        stackTraceText = intent.getStringExtra(KEY_STACK_TRACE) ?: getString(R.string.empty)
        crashReportMode = intent.getBooleanExtra(KEY_IS_A_CRASH, true)

        printStackTraceText(stackTraceText)

        autoSetStatusBarColor = false
        autoSetNavigationBarColor = false
        binding = ActivityCrashBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupTheme()

        deviceInfo = getDeviceInfo(this)
        displayText = buildString {
            append("${if (crashReportMode) "Crash Report" else "Internal Error"}:\n\n")
            append("$deviceInfo\n")
            append("$stackTraceText\n")
        }

        // display textview
        binding.crashText.text = displayText
        binding.crashText.setTextColor(primaryTextColor(nightMode))

        // button "copy to clipboard"
        binding.copyToClipboard.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("CRASH", displayText)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(this,
                           "${getString(R.string.copy_to_clipboard)}:\n${getString(R.string.success)}",
                           Toast.LENGTH_SHORT).show()
        }

        // save crash report
        externalCacheDir?.let { cacheDir ->
            lifecycleScope.launch(Dispatchers.IO + SupervisorJob()) {
                val file = File(cacheDir, "Error_Report_${currentDateTime()}.txt")
                file.writer().use { writer ->
                    writer.write(displayText)
                    writer.flush()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        attach(menu) {
            menuItem(0, R.id.nav_settings, 1, getString(R.string.action_settings)) {
                icon = getTintedDrawable(R.drawable.ic_settings_white_24dp, Color.WHITE)
                showAsActionFlag = SHOW_AS_ACTION_IF_ROOM
                onClick {
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            startActivity(
                                Intent(this@CrashActivity, SettingsActivity::class.java)
                            )
                        }, 80
                    )
                    true
                }
            }
            menuItem(0, NONE, 2, getString(R.string.clear_all_preference)) {
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    val dialog = alertDialog(context) {
                        title(R.string.clear_all_preference)
                        message(R.string.clear_all_preference_msg)
                        neutralButton(android.R.string.cancel)
                        positiveButton(R.string.clear_all_preference) { dialog ->
                            dialog.dismiss()
                            SettingDataManager.clearAllPreference()
                            Reboot.reboot(context)
                        }
                        builder.setCancelable(true)
                    }
                    dialog.show()
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        ?.setTextColor(getColor(MR.color.md_red_A700))
                    true
                }
            }
        }
        return true
    }

    companion object {
        private const val CRASH_PROCESS_NAME_SUFFIX = "crash"
        fun isCrashProcess(context: Context): Boolean {
            val manager = context.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
            val runningProcesses = manager.runningAppProcesses ?: return false
            val processInfo = runningProcesses.first { it.pid == Process.myPid() }
            return processInfo.processName.endsWith(CRASH_PROCESS_NAME_SUFFIX)
        }

        private fun printStackTraceText(stackTraceText: String) {
            Log.w("Crash", "Crashed!")
            Log.i("Crash", stackTraceText)
        }
    }
}
