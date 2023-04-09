package player.phonograph.ui.activities

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import lib.phonograph.activity.ToolbarActivity
import lib.phonograph.misc.Reboot
import mt.pref.ThemeColor
import mt.tint.setActivityToolbarColorAuto
import player.phonograph.App
import player.phonograph.R
import player.phonograph.migrate.DatabaseDataManger
import player.phonograph.migrate.SettingDataManager
import lib.phonograph.misc.CreateFileStorageAccessTool
import lib.phonograph.misc.ICreateFileStorageAccess
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenDocumentContract
import lib.phonograph.misc.OpenFileStorageAccessTool
import player.phonograph.migrate.backup.Backup
import player.phonograph.misc.menuProvider
import player.phonograph.ui.dialogs.BackupDataDialog
import player.phonograph.ui.fragments.SettingsFragment
import player.phonograph.util.coroutineToast
import player.phonograph.util.text.currentDateTime
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : ToolbarActivity(), ICreateFileStorageAccess, IOpenFileStorageAccess {

    override val openFileStorageAccessTool: OpenFileStorageAccessTool =
        OpenFileStorageAccessTool()
    override val createFileStorageAccessTool: CreateFileStorageAccessTool =
        CreateFileStorageAccessTool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        openFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        createFileStorageAccessTool.register(lifecycle, activityResultRegistry)

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
                    createFileStorageAccessTool.launch(
                        "phonograph_plus_databases_${currentDateTime()}.zip"
                    ) { uri ->
                        uri ?: return@launch
                        CoroutineScope(Dispatchers.IO).launch {
                            exportDatabase(uri).andReport()
                        }
                    }
                    true
                }
            }
            menuItem {
                itemId = R.id.action_import_data
                title = "${getString(R.string.import_)}${getString(R.string.databases)}"
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    openFileStorageAccessTool.launch(
                        OpenDocumentContract.Config(arrayOf("application/zip"))
                    ) { uri ->
                        uri ?: return@launch
                        CoroutineScope(Dispatchers.IO).launch {
                            importDatabase(uri).andReport()
                        }
                    }
                    true
                }
            }
            menuItem {
                itemId = R.id.action_export_preferences
                title = "${getString(R.string.export_)}${getString(R.string.preferences)}"
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    createFileStorageAccessTool.launch(
                        "phonograph_plus_settings_${currentDateTime()}.json"
                    ) { uri ->
                        uri ?: return@launch
                        CoroutineScope(Dispatchers.IO).launch {
                            exportSetting(uri).andReport()
                        }
                    }
                    true
                }
            }
            menuItem {
                itemId = R.id.action_import_preferences
                title = "${getString(R.string.import_)}${getString(R.string.preferences)}"
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    openFileStorageAccessTool.launch(
                        OpenDocumentContract.Config(arrayOf("application/json"))
                    ) { uri ->
                        uri ?: return@launch
                        CoroutineScope(Dispatchers.IO).launch {
                            importSetting(uri).andReport()
                        }
                    }
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
                        positiveButton(R.string.clear_all_preference) { dialog ->
                            dialog.dismiss()
                            SettingDataManager.clearAllPreference()
                            Reboot.reboot(context)
                        }
                        cancelOnTouchOutside(true)
                        getActionButton(WhichButton.POSITIVE).updateTextColor(getColor(R.color.md_red_A700))
                    }
                    true
                }
            }
            menuItem {
                titleRes(R.string.action_backup)
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    BackupDataDialog().show(supportFragmentManager, "BACKUP_DIALOG")
                    true
                }
            }

            menuItem {
                title = "Export All"
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    createFileStorageAccessTool.launch(
                        "phonograph_plus_backup_${currentDateTime()}.zip"
                    ) { uri ->
                        uri ?: return@launch
                        lifecycleScope.launch(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri, "wt")?.use {
                                Backup.exportBackupToArchive(context = this@SettingsActivity, targetOutputStream = it)
                            }
                        }
                    }

                    true
                }
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
        coroutineToast(App.instance, if (this) R.string.success else R.string.failed)
    }
}
