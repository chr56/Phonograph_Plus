/*
 *  Copyright (c) 2023 chr_56
 *
 */

package player.phonograph.ui.modules.setting

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.phonograph.misc.Reboot
import lib.storage.launcher.CreateFileStorageAccessDelegate
import lib.storage.launcher.ICreateFileStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDocumentContract
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.R
import player.phonograph.mechanism.SettingDataManager
import player.phonograph.mechanism.backup.Backup
import player.phonograph.ui.basis.ComposeActivity
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.DropDownMenuContent
import player.phonograph.ui.dialogs.BackupExportDialog
import player.phonograph.ui.dialogs.BackupImportDialog
import player.phonograph.ui.modules.explorer.PathSelectorContractTool
import player.phonograph.ui.modules.explorer.PathSelectorRequester
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
                Scaffold(
                    Modifier.statusBarsPadding(),
                    scaffoldState = scaffoldState,
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(stringResource(R.string.action_settings))
                            },
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
                                        Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_actions))
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
                        PhonographPreferenceScreen()
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

    @Composable
    private fun Menu() {
        val context = LocalContext.current
        DropDownMenuContent(
            listOf(
                menuItemClearAll(context),
                menuItemImport(context),
                menuItemExport(),
            )
        )
    }

    @Composable
    private fun menuItemClearAll(context: Context): Pair<String, Function0<Unit>> =
        stringResource(id = R.string.clear_all_preference) to {
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
                getActionButton(WhichButton.POSITIVE).updateTextColor(
                    getColor(util.theme.materials.R.color.md_red_A700)
                )
            }
        }

    @Composable
    private fun menuItemImport(context: Context): Pair<String, Function0<Unit>> =
        stringResource(id = R.string.action_import).format(stringResource(id = R.string.action_backup)) to {
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
        stringResource(id = R.string.action_export).format(stringResource(id = R.string.action_backup)) to {
            BackupExportDialog().show(supportFragmentManager, "EXPORT")
        }


    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val pathSelectorContractTool: PathSelectorContractTool = PathSelectorContractTool()
}