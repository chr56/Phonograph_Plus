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
import android.widget.Button
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import kotlin.system.exitProcess
import lib.phonograph.activity.ToolbarActivity
import player.phonograph.KEY_STACK_TRACE
import player.phonograph.R
import player.phonograph.settings.SettingManager
import player.phonograph.util.DeviceInfoUtil.getDeviceInfo

class CrashActivity : ToolbarActivity() {

    private lateinit var textView: TextView
    private lateinit var copyButton: Button
    private lateinit var resetAllButton: Button

    private fun setupTheme() {
        // statusbar theme
        setStatusbarColor(resources.getColor(R.color.md_grey_800, theme))

        // toolbar theme
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(resources.getColor(R.color.md_grey_700, theme))
        toolbar.title = getString(R.string.crash)
        setSupportActionBar(toolbar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        autoSetStatusBarColor = false
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)
        setupTheme()
        textView = findViewById(R.id.crash_text)

        // stack trace text
        val stackTraceText: String = intent.getStringExtra(KEY_STACK_TRACE) ?: getString(R.string.empty)
        // device data
        val deviceInfo: String = getDeviceInfo(this)
        // appended string
        val displayText = "Crash Report:\n\n$deviceInfo\n$stackTraceText\n"

        // display textview
        textView.text = displayText

        // button "copy to clipboard"
        copyButton = findViewById(R.id.copy_to_clipboard)
        copyButton.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("CRASH", displayText)
            clipboardManager.setPrimaryClip(clipData)
        }

        // button "clear all preference"
        resetAllButton = findViewById(R.id.action_clear_all_preference)
        resetAllButton.setOnClickListener {
            val dialog: MaterialDialog = MaterialDialog(this)
                .title(R.string.clear_all_preference)
                .message(R.string.clear_all_preference_msg)
                .negativeButton(android.R.string.cancel)
                .positiveButton(R.string.clear_all_preference) {
                    SettingManager(this).clearAllPreference()

                    Handler(Looper.getMainLooper()).postDelayed({
                        Process.killProcess(Process.myPid())
                        exitProcess(1)
                    }, 4000)
                }
                .cancelOnTouchOutside(true)
                .apply {
                    getActionButton(WhichButton.POSITIVE).updateTextColor(getColor(R.color.md_red_A700))
                }

            dialog.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.let { m ->
            m.add(NONE, R.id.nav_settings, 0, getString(R.string.action_settings)).also {
                it.setShowAsAction(SHOW_AS_ACTION_NEVER)
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
