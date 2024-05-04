/*
 *  Copyright (c) 2023 chr_56
 *
 */

package player.phonograph.ui.modules.setting

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import lib.activityresultcontract.CreateFileStorageAccessTool
import lib.activityresultcontract.ICreateFileStorageAccess
import lib.activityresultcontract.IOpenFileStorageAccess
import lib.activityresultcontract.OpenDocumentContract
import lib.activityresultcontract.OpenFileStorageAccessTool
import lib.phonograph.misc.Reboot
import player.phonograph.R
import player.phonograph.mechanism.SettingDataManager
import player.phonograph.mechanism.backup.Backup
import player.phonograph.ui.compose.ComposeThemeActivity
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
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream

class SettingsActivity : ComposeThemeActivity(),
                         ICreateFileStorageAccess, IOpenFileStorageAccess,
                         PathSelectorRequester {

    private val dropMenuState = mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        openFileStorageAccessTool.register(this)
        createFileStorageAccessTool.register(this)
        pathSelectorContractTool.register(this)
        super.onCreate(savedInstanceState)


        setContent {
            val scaffoldState = rememberScaffoldState()
            val highlightColor by primaryColor.collectAsState()
            PhonographTheme(highlightColor) {
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
                            backgroundColor = highlightColor

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
        DropDownMenuContent(listOf(stringResource(id = R.string.clear_all_preference) to {
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
                getActionButton(WhichButton.POSITIVE).updateTextColor(getColor(mt.color.R.color.md_red_A700))
            }
        }, stringResource(id = R.string.action_import).format(stringResource(id = R.string.action_backup)) to {
            openFileStorageAccessTool.launch(
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
        }, stringResource(id = R.string.action_export).format(stringResource(id = R.string.action_backup)) to {
            BackupExportDialog().show(supportFragmentManager, "EXPORT")
        }))
    }


    override val openFileStorageAccessTool: OpenFileStorageAccessTool = OpenFileStorageAccessTool()
    override val createFileStorageAccessTool: CreateFileStorageAccessTool = CreateFileStorageAccessTool()
    override val pathSelectorContractTool: PathSelectorContractTool = PathSelectorContractTool()
}