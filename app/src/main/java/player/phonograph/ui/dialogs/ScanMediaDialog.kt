/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.R
import player.phonograph.foundation.error.warning
import player.phonograph.foundation.notification.ProgressNotificationConnection
import player.phonograph.mechanism.scanner.MediaStoreScanner
import player.phonograph.model.notification.NOTIFICATION_CHANNEL_ID_DATABASE_SYNC
import player.phonograph.repo.room.DatabaseActions
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.LimitedDialog
import player.phonograph.ui.modules.explorer.PathSelectorContractTool
import player.phonograph.ui.modules.explorer.PathSelectorRequester
import player.phonograph.util.concurrent.coroutineToast
import player.phonograph.util.concurrent.lifecycleScopeOrNewOne
import player.phonograph.util.file.listPaths
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScanMediaDialog : ComposeViewDialogFragment() {

    @Composable
    override fun Content() {
        PhonographTheme {
            val helpDescriptionTextStyle =
                MaterialTheme.typography.body2.copy(
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    letterSpacing = 0.2.sp
                )
            LimitedDialog(onDismiss = ::dismiss) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 24.dp, horizontal = 24.dp)
                ) {
                    // Title
                    Text(
                        stringResource(R.string.action_scan_media),
                        modifier = Modifier.padding(bottom = 16.dp),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.h5,
                    )
                    // Content
                    Text(
                        stringResource(R.string.help_scan_media),
                        modifier = Modifier.fillMaxWidth(),
                        style = helpDescriptionTextStyle,
                    )
                    Button(
                        onClick = ::scanMedia,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.action_scan_media))
                    }
                    var rebuild by remember { mutableStateOf(false) }
                    Text(
                        stringResource(R.string.help_refresh_database),
                        modifier = Modifier.fillMaxWidth(),
                        style = helpDescriptionTextStyle,
                    )
                    Text(
                        stringResource(R.string.help_rebuild_database),
                        modifier = Modifier.fillMaxWidth(),
                        style = helpDescriptionTextStyle,
                    )
                    Row(Modifier.clickable { rebuild = !rebuild }) {
                        Checkbox(
                            rebuild, null,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Text(
                            stringResource(R.string.action_rebuild_database),
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                    Button(
                        onClick = { refreshOrRebuild(rebuild) },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .align(Alignment.End)
                    ) {
                        Text(
                            if (rebuild) stringResource(R.string.action_rebuild_database)
                            else stringResource(R.string.action_refresh_database)
                        )
                    }

                    // Buttons
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = ::dismiss,
                        modifier = Modifier
                            .align(Alignment.End)
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
        }
    }

    private fun scanMedia() {
        val fragmentActivity = requireActivity()
        val contractTool: PathSelectorContractTool? =
            (fragmentActivity as? PathSelectorRequester)?.pathSelectorContractTool
        contractTool?.launch(null) { path ->
            if (path != null) {
                fragmentActivity.lifecycleScope.launch {
                    val mediaStoreScanner = MediaStoreScanner(fragmentActivity)
                    try {
                        val paths = listPaths(path)
                        if (paths.isNotEmpty()) {
                            coroutineToast(fragmentActivity.applicationContext, R.string.action_scan_media)
                            mediaStoreScanner.scan(paths)
                        } else {
                            coroutineToast(fragmentActivity.applicationContext, R.string.msg_nothing_to_scan)
                        }
                    } catch (e: Exception) {
                        warning(fragmentActivity, "ScanMedia", "Failed to scan media", e)
                    }
                }
                dismiss()
            }
        }
    }

    private fun refreshOrRebuild(rebuild: Boolean) {
        if (rebuild) rebuildDatabase() else refreshDatabase()
    }

    private fun refreshDatabase() {
        val fragmentActivity = requireActivity()
        fragmentActivity.lifecycleScopeOrNewOne().launch(Dispatchers.IO) {
            val progress = ProgressNotificationConnection(
                fragmentActivity, R.string.action_refresh_database,
                channel = NOTIFICATION_CHANNEL_ID_DATABASE_SYNC,
            )
            progress.onStart()
            DatabaseActions.sync(
                fragmentActivity.applicationContext,
                MusicDatabase.koinInstance,
                progress,
                force = true
            )
            progress.onCompleted()
        }
        dismiss()
    }


    private fun rebuildDatabase() {
        val fragmentActivity = requireActivity()
        fragmentActivity.lifecycleScopeOrNewOne().launch(Dispatchers.IO) {
            val progress = ProgressNotificationConnection(
                fragmentActivity, R.string.action_rebuild_database,
                channel = NOTIFICATION_CHANNEL_ID_DATABASE_SYNC,
            )
            progress.onStart()
            DatabaseActions.rebuild(
                fragmentActivity.applicationContext,
                MusicDatabase.koinInstance,
                progress
            )
            progress.onCompleted()
        }
        dismiss()
    }

}