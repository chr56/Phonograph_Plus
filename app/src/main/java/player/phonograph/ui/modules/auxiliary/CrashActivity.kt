package player.phonograph.ui.modules.auxiliary

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import lib.phonograph.dialog.alertDialog
import lib.phonograph.misc.Reboot
import player.phonograph.R
import player.phonograph.databinding.ActivityCrashBinding
import player.phonograph.mechanism.SettingDataManager
import player.phonograph.model.CrashReport
import player.phonograph.model.CrashReport.Constant.CRASH_TYPE_CORRUPTED_DATA
import player.phonograph.model.CrashReport.Constant.CRASH_TYPE_INTERNAL_ERROR
import player.phonograph.ui.basis.ToolbarActivity
import player.phonograph.ui.modules.setting.SettingsActivity
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.dateTimeSuffixCompat
import player.phonograph.util.text.getDeviceInfo
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.systemDarkmode
import player.phonograph.util.theme.updateSystemBarsColor
import util.theme.color.primaryTextColor
import util.theme.materials.MaterialColor
import util.theme.view.toolbar.setToolbarColor
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
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
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class CrashActivity : ToolbarActivity() {

    private lateinit var binding: ActivityCrashBinding


    private var _report: CrashReport? = null
    private val report get() = _report!!

    private lateinit var deviceInfoText: String
    private lateinit var fullReportText: String

    override fun onCreate(savedInstanceState: Bundle?) {

        _report = IntentCompat.getParcelableExtra(intent, CrashReport.KEY, CrashReport::class.java)

        if (_report == null) {
            finish()
            return
        }

        printStackTraceText(report.stackTrace)

        binding = ActivityCrashBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupTheme()

        deviceInfoText = getDeviceInfo(this)
        fullReportText = buildString {
            append("$reportHead:\n\n")
            append("$deviceInfoText\n")
            val isNoteEmpty = report.note.isEmpty()
            val isStacktraceEmpty = report.stackTrace.isEmpty()
            if (!isNoteEmpty) {
                append("\nNote:\n")
                append("${report.note}\n")
            }
            if (!isStacktraceEmpty) {
                append("\nStacktrace:\n")
                append("${report.stackTrace}\n")
            }
            if (isNoteEmpty && isStacktraceEmpty) {
                append("\n\nNo message or stacktrace?\n")
                append("This should not happened. Please cat log manually!")
            }
        }

        // display textview
        binding.crashText.text = fullReportText
        binding.crashText.setTextColor(primaryTextColor(systemDarkmode(resources)))

        // button "copy to clipboard"
        binding.copyToClipboard.setOnClickListener {
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("CRASH", fullReportText)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(
                this,
                "${getString(R.string.copy_to_clipboard)}:\n${getString(R.string.success)}",
                Toast.LENGTH_SHORT
            ).show()
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.copyToClipboard) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }

        // save crash report
        externalCacheDir?.let { cacheDir ->
            lifecycleScope.launch(Dispatchers.IO + SupervisorJob()) {
                val file = File(cacheDir, "Error_Report_${dateTimeSuffixCompat(currentDate())}.txt")
                file.writer().use { writer ->
                    writer.write(fullReportText)
                    writer.flush()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        attach(menu) {
            menuItem(0, R.id.nav_settings, 1, getString(R.string.action_settings)) {
                icon = getTintedDrawable(
                    R.drawable.ic_settings_white_24dp,
                    context.primaryTextColor(colorPrimary)
                )
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
                        ?.setTextColor(MaterialColor.Red._A700.asColor)
                    true
                }
            }
        }
        return true
    }

    companion object {
        private const val CRASH_PROCESS_NAME_SUFFIX = "crash"
        fun isCrashProcess(context: Context): Boolean {
            val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val runningProcesses = manager.runningAppProcesses ?: return false
            val processInfo = runningProcesses.first { it.pid == Process.myPid() }
            return processInfo.processName.endsWith(CRASH_PROCESS_NAME_SUFFIX)
        }

        private fun printStackTraceText(stackTraceText: String) {
            Log.w("Crash", "Crashed!")
            Log.i("Crash", stackTraceText)
        }
    }

    //region Theme

    private fun setupTheme() {
        // System UI
        updateSystemBarsColor(colorPrimaryDeep, Color.TRANSPARENT)

        // toolbar theme
        binding.toolbar.apply {
            setToolbarColor(this, colorPrimary)
            title = getString(titleRes)
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

    private val colorPrimaryDeep: Int
        get() = when (report.type) {
            CRASH_TYPE_INTERNAL_ERROR -> MaterialColor.Grey._800.asColor
            CRASH_TYPE_CORRUPTED_DATA -> MaterialColor.Purple._800.asColor
            else                      -> MaterialColor.DeepOrange._900.asColor
        }

    private val colorPrimary: Int
        get() = when (report.type) {
            CRASH_TYPE_INTERNAL_ERROR -> MaterialColor.Grey._700.asColor
            CRASH_TYPE_CORRUPTED_DATA -> MaterialColor.Purple._700.asColor
            else                      -> MaterialColor.DeepOrange._700.asColor
        }

    private val titleRes: Int
        get() = when (report.type) {
            CRASH_TYPE_INTERNAL_ERROR -> R.string.internal_error
            CRASH_TYPE_CORRUPTED_DATA -> R.string.internal_error
            else                      -> R.string.crash
        }

    private val reportHead: String
        get() = when (report.type) {
            CRASH_TYPE_INTERNAL_ERROR -> "Internal Error"
            CRASH_TYPE_CORRUPTED_DATA -> "Corrupted Data"
            else                      -> "Crash Report"
        }
    //endregion
}
