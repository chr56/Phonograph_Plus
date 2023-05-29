/*
 *  Copyright (c) 2023 chr_56
 *
 */

package player.phonograph.ui.compose.settings

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import lib.phonograph.misc.CreateFileStorageAccessTool
import lib.phonograph.misc.ICreateFileStorageAccess
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenDocumentContract
import lib.phonograph.misc.OpenFileStorageAccessTool
import lib.phonograph.misc.Reboot
import player.phonograph.R
import player.phonograph.mechanism.SettingDataManager
import player.phonograph.mechanism.backup.Backup
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import player.phonograph.ui.compose.components.DropDownMenuContent
import player.phonograph.ui.dialogs.BackupExportDialog
import player.phonograph.ui.dialogs.BackupImportDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream

class SettingsActivity : ComposeToolbarActivity(), ICreateFileStorageAccess, IOpenFileStorageAccess {

    private val dropMenuState = mutableStateOf(false)

    @Composable
    override fun SetUpContent() {
        Box {
            val state = remember { dropMenuState }
            PhonographPreferenceScreen()
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                DropdownMenu(
                    expanded = state.value,
                    onDismissRequest = { state.value = false })
                {
                    Menu()
                }
            }
        }
    }

    override val title: String
        get() = getString(R.string.action_settings)

    override val toolbarActions: @Composable RowScope.() -> Unit = @Composable {
        IconButton(
            content = {
                Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_actions))
            },
            onClick = {
                dropMenuState.value = true
            }
        )
    }

    @Composable
    private fun Menu() {
        val context = LocalContext.current
        DropDownMenuContent(
            listOf(
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
                        getActionButton(WhichButton.POSITIVE).updateTextColor(getColor(mt.color.R.color.md_red_A700))
                    }
                },
                stringResource(id = R.string.action_import).format(stringResource(id = R.string.action_backup)) to {
                    openFileStorageAccessTool.launch(
                        OpenDocumentContract.Config(arrayOf("*/*"))
                    ) { uri ->
                        uri ?: return@launch
                        lifecycleScope.launch(Dispatchers.IO) {
                            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                                FileInputStream(it.fileDescriptor).use { stream ->
                                    val sessionId =
                                        Backup.Import.startImportBackupFromArchive(
                                            context = this@SettingsActivity,
                                            stream
                                        )
                                    BackupImportDialog.newInstance(sessionId)
                                        .show(supportFragmentManager, "IMPORT")
                                }
                            }
                        }
                    }
                },
                stringResource(id = R.string.action_export).format(stringResource(id = R.string.action_backup)) to {
                    BackupExportDialog().show(supportFragmentManager, "EXPORT")
                },
                stringResource(id = R.string.action_settings) to {
                    startActivity(
                        Intent(
                            this@SettingsActivity,
                            player.phonograph.ui.activities.SettingsActivity::class.java
                        )
                    )
                },
            )
        )
    }


    override val openFileStorageAccessTool: OpenFileStorageAccessTool =
        OpenFileStorageAccessTool()
    override val createFileStorageAccessTool: CreateFileStorageAccessTool =
        CreateFileStorageAccessTool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        createFileStorageAccessTool.register(lifecycle, activityResultRegistry)
    }
}