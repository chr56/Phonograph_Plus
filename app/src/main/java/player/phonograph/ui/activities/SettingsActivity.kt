package player.phonograph.ui.activities

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import lib.phonograph.activity.ToolbarActivity
import mt.pref.ThemeColor
import mt.tint.setActivityToolbarColorAuto
import player.phonograph.App
import player.phonograph.R
import player.phonograph.migrate.SettingDataManager
import player.phonograph.misc.OpenDocumentContract
import player.phonograph.misc.menuProvider
import player.phonograph.migrate.DatabaseDataManger
import player.phonograph.ui.fragments.SettingsFragment
import player.phonograph.util.CoroutineUtil
import player.phonograph.util.TimeUtil.currentDateTime
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.view.Menu
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : ToolbarActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(ThemeColor.primaryColor(this))
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu))
        setActivityToolbarColorAuto(toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, SettingsFragment()).commit()
        } else {
            val frag =
                supportFragmentManager.findFragmentById(R.id.content_frame) as SettingsFragment?
            frag?.invalidateSettings()
        }
    }

    private fun setupMenu(menu: Menu) {
        attach(from = menu) {
            menuItem {
                itemId = R.id.action_export_data
                title = "${getString(R.string.export_)}${getString(R.string.databases)}"
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    createAction = { uri -> exportDatabase(uri) }
                    createLauncher.launch("phonograph_plus_databases_${currentDateTime()}.zip")
                    true
                }
            }
            menuItem {
                itemId = R.id.action_import_data
                title = "${getString(R.string.import_)}${getString(R.string.databases)}"
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    openAction = { uri -> importDatabase(uri) }
                    openLauncher.launch(OpenDocumentContract.Cfg(null, arrayOf("application/zip")))
                    true
                }
            }
            menuItem {
                itemId = R.id.action_export_preferences
                title = "${getString(R.string.export_)}${getString(R.string.preferences)}"
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    createAction = { uri -> exportSetting(uri) }
                    createLauncher.launch("phonograph_plus_settings_${currentDateTime()}.json")
                    true
                }
            }
            menuItem {
                itemId = R.id.action_import_preferences
                title = "${getString(R.string.import_)}${getString(R.string.preferences)}"
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    openAction = { uri -> importSetting(uri) }
                    openLauncher.launch(OpenDocumentContract.Cfg(null, arrayOf("application/json")))
                    true
                }
            }
            menuItem {
                itemId = R.id.action_clear_all_preference
                title = getString(R.string.clear_all_preference)
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    MaterialDialog(context).show {
                        title(R.string.clear_all_preference)
                        message(R.string.clear_all_preference_msg)
                        negativeButton(android.R.string.cancel)
                        positiveButton(R.string.clear_all_preference) {
                            SettingDataManager.clearAllPreference()

                            Handler().postDelayed(
                                {
                                    Process.killProcess(Process.myPid())
                                    exitProcess(1)
                                }, 4000
                            )
                        }
                        cancelOnTouchOutside(true)
                        getActionButton(WhichButton.POSITIVE).updateTextColor(getColor(R.color.md_red_A700))
                    }
                    true
                }
            }
        }
    }

    private lateinit var createAction: (Uri) -> Boolean
    private lateinit var openAction: (Uri) -> Boolean

    private val createLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
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

    private fun exportDatabase(uri: Uri): Boolean =
        DatabaseDataManger.exportDatabases(uri, App.instance)

    private fun importDatabase(uri: Uri): Boolean =
        DatabaseDataManger.importDatabases(uri, App.instance)

    private fun exportSetting(uri: Uri): Boolean =
        SettingDataManager.exportSettings(uri, App.instance)

    private fun importSetting(uri: Uri): Boolean =
        SettingDataManager.importSetting(uri, App.instance)

    private suspend fun Boolean.andReport() {
        CoroutineUtil.coroutineToast(App.instance, if (this) R.string.success else R.string.failed)
    }
}
