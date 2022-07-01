package player.phonograph.ui.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.Menu
import android.view.Menu.NONE
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import androidx.appcompat.widget.Toolbar
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import kotlin.system.exitProcess
import lib.phonograph.activity.ToolbarActivity
import player.phonograph.KEY_STACK_TRACE
import player.phonograph.R
import player.phonograph.databinding.ActivityCrashBinding
import player.phonograph.settings.SettingManager
import player.phonograph.util.DeviceInfoUtil.getDeviceInfo

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

    override fun onCreate(savedInstanceState: Bundle?) {
        autoSetStatusBarColor = false
        autoSetNavigationBarColor = false
        binding = ActivityCrashBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupTheme()

        // stack trace text
        val stackTraceText: String = intent.getStringExtra(KEY_STACK_TRACE) ?: getString(R.string.empty)
        // device data
        val deviceInfo: String = getDeviceInfo(this)
        // appended string
        val displayText = "Crash Report:\n\n$deviceInfo\n$stackTraceText\n"

        // display textview
        binding.crashText.text = displayText

        // button "copy to clipboard"
        binding.copyToClipboard.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("CRASH", displayText)
            clipboardManager.setPrimaryClip(clipData)
        }

        // button "clear all preference"
        binding.actionClearAllPreference.setOnClickListener {
            MaterialDialog(this).show {
                title(R.string.clear_all_preference)
                message(R.string.clear_all_preference_msg)
                cancelOnTouchOutside(true)
                negativeButton(android.R.string.cancel)
                positiveButton(R.string.clear_all_preference) {
                    SettingManager(this@CrashActivity).clearAllPreference()
                    Handler(Looper.getMainLooper()).postDelayed({
                        Process.killProcess(Process.myPid())
                        exitProcess(1)
                    }, 4000)
                }
                apply {
                    getActionButton(WhichButton.POSITIVE).updateTextColor(getColor(R.color.md_red_A700))
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        with(menu) {
            add(NONE, R.id.nav_settings, 0, getString(R.string.action_settings)).apply {
                setShowAsAction(SHOW_AS_ACTION_NEVER)
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, SettingsActivity::class.java))
                }, 80)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
