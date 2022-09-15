package player.phonograph.ui.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.appcompat.widget.Toolbar
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lib.phonograph.activity.ToolbarActivity
import mt.pref.ThemeColor
import mt.util.color.primaryTextColor
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.notification.ErrorNotification.KEY_STACK_TRACE
import player.phonograph.R
import player.phonograph.databinding.ActivityCrashBinding
import player.phonograph.settings.SettingManager
import player.phonograph.util.DeviceInfoUtil.getDeviceInfo
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.PhonographColorUtil.nightMode
import player.phonograph.util.TimeUtil.currentDateTime
import java.io.File
import java.io.FileOutputStream
import kotlin.system.exitProcess

class CrashActivity : ToolbarActivity() {

    private lateinit var binding: ActivityCrashBinding

    private fun setupTheme() {
        // statusbar theme
        updateAllColors(resources.getColor(R.color.md_grey_800, theme))

        // toolbar theme
        findViewById<Toolbar>(R.id.toolbar).apply {
            setBackgroundColor(resources.getColor(R.color.md_grey_700, theme))
            title = getString(R.string.crash)
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
        autoSetStatusBarColor = false
        autoSetNavigationBarColor = false
        binding = ActivityCrashBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupTheme()

        stackTraceText = intent.getStringExtra(KEY_STACK_TRACE) ?: getString(R.string.empty)
        deviceInfo = getDeviceInfo(this)
        displayText = "Crash Report:\n\n$deviceInfo\n$stackTraceText\n"

        // display textview
        binding.crashText.text = displayText
        binding.crashText.setTextColor(primaryTextColor(resources.nightMode))

        // button "copy to clipboard"
        binding.copyToClipboard.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("CRASH", displayText)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(this, "${getString(R.string.copy_to_clipboard)}:\n${getString(R.string.success)}", Toast.LENGTH_SHORT).show()
        }

        // save crash report
        externalCacheDir?.let { cacheDir ->
            coroutineScope.launch(Dispatchers.IO) {
                val file = File(cacheDir, "Crash_Report_${currentDateTime()}.txt")
                FileOutputStream(file).use {
                    it.writer().apply {
                        write(displayText)
                        flush()
                        close()
                    }
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
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(Intent(this@CrashActivity, SettingsActivity::class.java))
                    }, 80)
                    true
                }
            }
            menuItem(0, NONE, 2, getString(R.string.clear_all_preference)) {
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    MaterialDialog(this@CrashActivity).show {
                        title(R.string.clear_all_preference)
                        message(R.string.clear_all_preference_msg)
                        cancelOnTouchOutside(true)
                        negativeButton(android.R.string.cancel)
                        positiveButton(R.string.clear_all_preference) {
                            val context = this@CrashActivity.applicationContext
                            SettingManager(context).clearAllPreference(context)
                            ThemeColor.editTheme(context).clearAllPreference() // lib
                            Handler(Looper.getMainLooper()).postDelayed({
                                Process.killProcess(Process.myPid())
                                exitProcess(1)
                            }, 4000)
                        }
                        apply {
                            getActionButton(WhichButton.POSITIVE).updateTextColor(getColor(R.color.md_red_A700))
                        }
                    }
                    true
                }
            }
        }
        return true
    }

    private val coroutineScope = CoroutineScope(SupervisorJob())
}
