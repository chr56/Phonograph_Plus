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
import player.phonograph.mechanism.SettingDataManager
import lib.phonograph.misc.CreateFileStorageAccessTool
import lib.phonograph.misc.ICreateFileStorageAccess
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenDocumentContract
import lib.phonograph.misc.OpenFileStorageAccessTool
import player.phonograph.mechanism.backup.Backup
import lib.phonograph.misc.menuProvider
import player.phonograph.ui.dialogs.BackupExportDialog
import player.phonograph.ui.dialogs.BackupImportDialog
import player.phonograph.ui.fragments.SettingsFragment
import player.phonograph.util.coroutineToast
import player.phonograph.util.text.currentDateTime
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem.SHOW_AS_ACTION_NEVER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import mt.color.R as MR

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
                itemId = R.id.action_export_preferences
                title = getString(R.string.action_export, getString(R.string.preferences))
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    createFileStorageAccessTool.launch(
                        "phonograph_plus_settings_${currentDateTime()}.json"
                    ) { uri ->
                        uri ?: return@launch
                        CoroutineScope(Dispatchers.IO).launch {
                            SettingDataManager.exportSettings(uri, context).andReport()
                        }
                    }
                    true
                }
            }
            menuItem {
                itemId = R.id.action_import_preferences
                title = getString(R.string.action_import, getString(R.string.preferences))
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    openFileStorageAccessTool.launch(
                        OpenDocumentContract.Config(arrayOf("application/json"))
                    ) { uri ->
                        uri ?: return@launch
                        CoroutineScope(Dispatchers.IO).launch {
                            SettingDataManager.importSetting(uri, context).andReport()
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
                        getActionButton(WhichButton.POSITIVE).updateTextColor(getColor(MR.color.md_red_A700))
                    }
                    true
                }
            }
            menuItem {
                title = getString(R.string.action_export, getString(R.string.action_backup))
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    BackupExportDialog().show(supportFragmentManager, "EXPORT")
                    true
                }
            }

            menuItem {
                title = getString(R.string.action_import, getString(R.string.action_backup))
                showAsActionFlag = SHOW_AS_ACTION_NEVER
                onClick {
                    openFileStorageAccessTool.launch(
                        OpenDocumentContract.Config(arrayOf("*/*"))
                    ) { uri ->
                        uri ?: return@launch
                        lifecycleScope.launch(Dispatchers.IO) {
                            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                                FileInputStream(it.fileDescriptor).use { stream ->
                                    val sessionId =
                                        Backup.Import.startImportBackupFromArchive(context = this@SettingsActivity, stream)
                                    BackupImportDialog.newInstance(sessionId).show(supportFragmentManager, "IMPORT")
                                }
                            }
                        }
                    }

                    true
                }
            }

            menuItem {
                title = getString(R.string.action_settings)
                onClick {
                    startActivity(Intent(this@SettingsActivity, player.phonograph.ui.compose.settings.SettingsActivity::class.java))
                    true
                }
            }
        }
    }

    private suspend fun Boolean.andReport() {
        coroutineToast(App.instance, if (this) R.string.success else R.string.failed)
    }
}
