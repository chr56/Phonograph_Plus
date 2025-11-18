/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import lib.storage.launcher.ICreateFileStorageAccessible
import player.phonograph.R
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.backup.Backup
import player.phonograph.model.backup.BackupItem
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.ActionItem
import player.phonograph.ui.compose.components.AdvancedDialogFrame
import player.phonograph.ui.compose.components.LimitedDialog
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.dateTimeSuffixCompat
import player.phonograph.util.theme.tintButtons
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.view.LayoutInflater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupExportDialog : ComposeViewDialogFragment() {

    private var adapter: BackupChooserAdapter? = null

    @Composable
    override fun Content() {
        PhonographTheme {
            LimitedDialog(onDismiss = ::dismiss) {
                AdvancedDialogFrame(
                    modifier = Modifier,
                    title = stringResource(
                        R.string.action_export,
                        stringResource(R.string.label_backup)
                    ),
                    navigationButtonIcon= rememberVectorPainter(Icons.Default.Close),
                    onDismissRequest = ::dismiss,
                    actions = listOf(
                        ActionItem(
                            Icons.Default.Check,
                            textRes = android.R.string.ok,
                            onClick = { execute() }
                        )
                    ),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { context ->
                            @SuppressLint("UseGetLayoutInflater", "InflateParams")
                            val view = LayoutInflater.from(context).inflate(R.layout.recycler_view_wrapped, null)
                            val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)

                            val allBackupConfig =
                                BackupItem.entries.filter { !it.deprecated }
                            val enabledBackupConfig =
                                BackupItem.entries.filter { !it.deprecated && it.enabledByDefault }

                            val configAdapter =
                                BackupChooserAdapter(enabledBackupConfig, allBackupConfig).also { it.init() }

                            adapter = configAdapter

                            recyclerView.layoutManager = LinearLayoutManager(context)
                            recyclerView.adapter = adapter
                            configAdapter.attachToRecyclerView(recyclerView)

                            view
                        }
                    )
                }
            }
        }
    }

    private fun execute() {
        val selected = adapter?.currentConfig
        if (selected.isNullOrEmpty()) return

        val host = requireActivity()
        if (host !is ICreateFileStorageAccessible) return

        host.createFileStorageAccessDelegate.launch(defaultBackupName()) { uri ->
            uri ?: return@launch
            lifecycleScope.launch(Dispatchers.IO) {
                host.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    val result =
                        try {
                            Backup.Export.exportBackupToArchive(host, selected, outputStream)
                            true
                        } catch (e: Exception) {
                            warning(host, TAG, host.getString(R.string.failed), e)
                            false
                        }
                    withContext(Dispatchers.Main) {
                        dismiss()
                        AlertDialog.Builder(host)
                            .setTitle(R.string.label_backup)
                            .setMessage(host.getString(if (result) R.string.state_completed else R.string.failed))
                            .setPositiveButton(android.R.string.ok) { _, _ -> }
                            .create().tintButtons().show()

                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
    }

    companion object {
        private const val TAG = "BackupExportDialog"
        private fun defaultBackupName(): String = "phonograph_plus_backup_${dateTimeSuffixCompat(currentDate())}.zip"
    }
}