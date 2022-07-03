package player.phonograph.ui.activities

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.github.chr56.android.menu_dsl.menu
import com.github.chr56.android.menu_dsl.menuItem
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lib.phonograph.activity.ToolbarActivity
import player.phonograph.App
import player.phonograph.R
import player.phonograph.misc.OpenDocumentContract
import player.phonograph.provider.DatabaseManger
import player.phonograph.settings.SettingManager
import player.phonograph.ui.fragments.SettingsFragment
import player.phonograph.util.Util
import player.phonograph.util.Util.currentDateTime
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer

class SettingsActivity : ToolbarActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(ThemeColor.primaryColor(this))
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        Themer.setActivityToolbarColorAuto(this, toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, SettingsFragment()).commit()
        } else {
            val frag =
                supportFragmentManager.findFragmentById(R.id.content_frame) as SettingsFragment?
            frag?.invalidateSettings()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu(from = menu) {
            menuItem {
                itemId = R.id.action_export_data
                title = "${getString(R.string.export_)}${getString(R.string.databases)}"
                showAsActionFlag = SHOW_AS_ACTION_NEVER
            }
            menuItem {
                itemId = R.id.action_import_data
                title = "${getString(R.string.import_)}${getString(R.string.databases)}"
                showAsActionFlag = SHOW_AS_ACTION_NEVER
            }
            menuItem {
                itemId = R.id.action_export_preferences
                title = "${getString(R.string.export_)}${getString(R.string.preferences)}"
                showAsActionFlag = SHOW_AS_ACTION_NEVER
            }
            menuItem {
                itemId = R.id.action_import_preferences
                title = "${getString(R.string.import_)}${getString(R.string.preferences)}"
                showAsActionFlag = SHOW_AS_ACTION_NEVER
            }
            menuItem {
                itemId = R.id.action_clear_all_preference
                title = getString(R.string.clear_all_preference)
                showAsActionFlag = SHOW_AS_ACTION_NEVER
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_export_data -> {
                createAction = { uri -> exportDatabase(uri) }
                createLauncher.launch("phonograph_plus_databases_${currentDateTime()}.zip")
                return true
            }
            R.id.action_import_data -> {
                openAction = { uri -> importDatabase(uri) }
                openLauncher.launch(OpenDocumentContract.Cfg(null, arrayOf("application/zip")))
                return true
            }
            R.id.action_export_preferences -> {
                createAction = { uri -> exportSetting(uri) }
                createLauncher.launch("phonograph_plus_settings_${currentDateTime()}.json")
                return true
            }
            R.id.action_import_preferences -> {
                openAction = { uri -> importSetting(uri) }
                openLauncher.launch(OpenDocumentContract.Cfg(null, arrayOf("application/json")))
                return true
            }
            R.id.action_clear_all_preference -> {
                MaterialDialog(this).show {
                    title(R.string.clear_all_preference)
                    message(R.string.clear_all_preference_msg)
                    negativeButton(android.R.string.cancel)
                    positiveButton(R.string.clear_all_preference) {
                        SettingManager(this@SettingsActivity.applicationContext).clearAllPreference()

                        Handler().postDelayed({
                            Process.killProcess(Process.myPid())
                            exitProcess(1)
                        }, 4000)
                    }
                    cancelOnTouchOutside(true)
                    getActionButton(WhichButton.POSITIVE).updateTextColor(getColor(R.color.md_red_A700))
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private lateinit var createAction: (Uri) -> Boolean
    private lateinit var openAction: (Uri) -> Boolean

    private val createLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
        it?.let { uri ->
            CoroutineScope(Dispatchers.IO).launch {
                createAction(uri).andReport()
            }
        }
    }
    private val openLauncher = registerForActivityResult(OpenDocumentContract()) {
        it?.let { uri ->
            CoroutineScope(Dispatchers.IO).launch {
                openAction(uri).andReport()
            }
        }
    }

    private fun exportDatabase(uri: Uri): Boolean {
        return DatabaseManger(App.instance).exportDatabases(uri)
    }
    private fun importDatabase(uri: Uri): Boolean {
        return DatabaseManger(App.instance).importDatabases(uri)
    }
    private fun exportSetting(uri: Uri): Boolean {
        return SettingManager(App.instance).exportSettings(uri)
    }
    private fun importSetting(uri: Uri): Boolean {
        return SettingManager(App.instance).importSetting(uri)
    }
    private suspend fun Boolean.andReport() {
        Util.coroutineToast(App.instance, if (this) R.string.success else R.string.failed)
    }
}
