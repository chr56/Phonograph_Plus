package player.phonograph.ui.activities

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import lib.phonograph.activity.ToolbarActivity
import mt.util.color.primaryTextColor
import player.phonograph.R
import player.phonograph.databinding.ActivityCrashBinding
import player.phonograph.notification.ErrorNotification.KEY_IS_A_CRASH
import player.phonograph.notification.ErrorNotification.KEY_STACK_TRACE
import player.phonograph.settings.SettingManager
import player.phonograph.util.DeviceInfoUtil.getDeviceInfo
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.PhonographColorUtil.nightMode
import player.phonograph.util.TimeUtil.currentDateTime
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.Menu
import android.view.Menu.NONE
import android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import android.widget.Toast
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class CrashActivity : ToolbarActivity() {

    private lateinit var binding: ActivityCrashBinding

    private var crashReportMode = true

    private fun setupTheme() {
        // statusbar theme
        updateAllColors(resources.getColor(R.color.md_grey_800, theme))

        // toolbar theme
        findViewById<Toolbar>(R.id.toolbar).apply {
            setBackgroundColor(resources.getColor(R.color.md_grey_700, theme))
            title = if (crashReportMode) getString(R.string.crash) else "Internal Error"
            setSupportActionBar(this)
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
        binding.crashText.setTextColor(primaryTextColor(resources.nightMode))

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
            coroutineScope.launch(Dispatchers.IO) {
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
                    val dialog = AlertDialog.Builder(context)
                        .setTitle(R.string.clear_all_preference)
                        .setMessage(R.string.clear_all_preference_msg)
                        .setCancelable(true)
                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                        .setPositiveButton(R.string.clear_all_preference) { dialog, _: Int ->
                            SettingManager(this@CrashActivity).clearAllPreference()
                            Handler(Looper.getMainLooper()).postDelayed(
                                {
                                    Process.killProcess(Process.myPid())
                                    exitProcess(1)
                                }, 2000
                            )
                        }
                        .create()
                    dialog.show()
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.md_red_A700))
                    true
                }
            }
        }
        return true
    }

    private val coroutineScope by lazy { CoroutineScope(SupervisorJob()) }
}
