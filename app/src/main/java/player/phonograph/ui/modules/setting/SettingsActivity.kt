/*
 *  Copyright (c) 2023 chr_56
 *
 */

package player.phonograph.ui.modules.setting

import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.storage.launcher.CreateFileStorageAccessDelegate
import lib.storage.launcher.ICreateFileStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDocumentContract
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.R
import player.phonograph.foundation.Reboot
import player.phonograph.mechanism.backup.Backup
import player.phonograph.settings.Setting
import player.phonograph.ui.basis.ComposeActivity
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.DropDownMenuContent
import player.phonograph.ui.compose.components.SystemBarsPadded
import player.phonograph.ui.dialogs.BackupExportDialog
import player.phonograph.ui.dialogs.BackupImportDialog
import player.phonograph.ui.dialogs.DatabaseMaintenanceDialog
import player.phonograph.ui.modules.explorer.PathSelectorContractTool
import player.phonograph.ui.modules.explorer.PathSelectorRequester
import util.theme.materials.MaterialColor
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream

class SettingsActivity : ComposeActivity(),
                         ICreateFileStorageAccessible, IOpenFileStorageAccessible,
                         PathSelectorRequester {

    private val dropMenuState = mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        registerActivityResultLauncherDelegate(
            openFileStorageAccessDelegate,
            createFileStorageAccessDelegate
        )
        pathSelectorContractTool.register(this)
        super.onCreate(savedInstanceState)


        setContent {
            val scaffoldState = rememberScaffoldState()
            PhonographTheme {
                SystemBarsPadded {
                    var title by remember { mutableStateOf(resources.getString(R.string.action_settings)) }
                    Scaffold(
                        scaffoldState = scaffoldState,
                        topBar = {
                            TopAppBar(
                                title = { Text(title) },
                                navigationIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Default.ArrowBack, null,
                                        Modifier
                                            .clickable {
                                                onBackPressedDispatcher.onBackPressed()
                                            }
                                            .padding(16.dp)
                                    )
                                },
                                actions = {
                                    IconButton(
                                        content = {
                                            Icon(Icons.Default.MoreVert, stringResource(id = R.string.action_more))
                                        },
                                        onClick = {
                                            dropMenuState.value = true
                                        }
                                    )
                                },
                                backgroundColor = MaterialTheme.colors.primary
                            )
                        },
                    ) {
                        Box(Modifier.padding(it)) {
                            val state = remember { dropMenuState }
                            PhonographPreferenceScreen(onBackPressedDispatcher) { title = it }
                            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                DropdownMenu(expanded = state.value, onDismissRequest = { state.value = false }) {
                                    Menu()
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    @Composable
    private fun Menu() {
        val context = LocalContext.current
        DropDownMenuContent(
            listOf(
                menuItemClearAll(context),
                menuItemImport(context),
                menuItemExport(),
                menuItemDeleteDatabase(),
            )
        )
    }

    @Composable
    private fun menuItemClearAll(context: Context): Pair<String, Function0<Unit>> =
        stringResource(id = R.string.action_clear_all_preference) to {
            AlertDialog.Builder(context)
                .setTitle(R.string.action_clear_all_preference)
                .setMessage(R.string.warning_clear_all_preference_msg)
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.action_clear_all_preference) { dialog, _ ->
                    dialog.dismiss()
                    runBlocking {
                        if (Setting(context).clearAll()) {
                            Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()
                            Reboot.reboot(context)
                        } else {
                            Toast.makeText(context, R.string.failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .create().apply {
                    setOnShowListener {
                        (it as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                            MaterialColor.Red._A700.asColor
                        )
                    }
                }.show()
        }

    @Composable
    private fun menuItemImport(context: Context): Pair<String, Function0<Unit>> =
        stringResource(id = R.string.action_import).format(stringResource(id = R.string.label_backup)) to {
            openFileStorageAccessDelegate.launch(
                OpenDocumentContract.Config(arrayOf("*/*"))
            ) { uri ->
                uri ?: return@launch
                lifecycleScope.launch(Dispatchers.IO) {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use {
                        FileInputStream(it.fileDescriptor).use { stream ->
                            val sessionId = Backup.Import.startImportBackupFromArchive(
                                context = this@SettingsActivity, stream
                            )
                            BackupImportDialog.newInstance(sessionId).show(supportFragmentManager, "IMPORT")
                        }
                    }
                }
            }
        }

    @Composable
    private fun menuItemExport(): Pair<String, Function0<Unit>> =
        stringResource(id = R.string.action_export).format(stringResource(id = R.string.label_backup)) to {
            BackupExportDialog().show(supportFragmentManager, "EXPORT")
        }

    @Composable
    private fun menuItemDeleteDatabase(): Pair<String, Function0<Unit>> =
        stringResource(R.string.label_database_maintenance) to {
            DatabaseMaintenanceDialog.create().show(supportFragmentManager, "DATABASE_MAINTENANCE")
        }


    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val pathSelectorContractTool: PathSelectorContractTool = PathSelectorContractTool()
}