/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.R
import player.phonograph.foundation.Reboot
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.backup.Backup
import player.phonograph.settings.PrerequisiteSetting
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.ActionItem
import player.phonograph.ui.compose.components.AdvancedDialogFrame
import player.phonograph.ui.compose.components.LimitedDialog
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStateAtLeast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupImportDialog : ComposeViewDialogFragment() {

    private var sessionId: Long = 0

    private var adapter: BackupChooserAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionId = arguments?.getLong(KEY_SESSION) ?: throw IllegalArgumentException("No session id!")
    }


    @Composable
    override fun Content() {
        PhonographTheme {
            LimitedDialog(onDismiss = ::dismiss) {
                AdvancedDialogFrame(
                    modifier = Modifier,
                    title = stringResource(
                        R.string.action_import,
                        stringResource(R.string.label_backup)
                    ),
                    navigationButtonIcon = rememberVectorPainter(Icons.Default.Close),
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

                            val manifest = Backup.Import.readManifest(requireActivity(), sessionId)
                            if (manifest != null) {
                                val items = manifest.files.map { it.key }

                                val backupChooserAdapter = BackupChooserAdapter(items, items).also { it.init() }

                                adapter = backupChooserAdapter

                                recyclerView.layoutManager = LinearLayoutManager(context)
                                recyclerView.adapter = adapter
                                backupChooserAdapter.attachToRecyclerView(recyclerView)
                            }

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

        val processDialog = ProgressDialog.newInstance(getString(R.string.label_backup))
        processDialog.show(host.supportFragmentManager, "ProgressDialog")
        host.lifecycleScope.launch(Dispatchers.IO) {
            val onUpdateProgress = fun(currentItem: CharSequence) {
                launch {
                    processDialog.lifecycle.withStateAtLeast(Lifecycle.State.STARTED) {
                        val context = processDialog.requireContext()
                        processDialog.currentTextState.update {
                            context.getString(R.string.action_import, currentItem.toString())
                        }
                    }
                }
            }
            val result =
                try {
                    Backup.Import.executeImport(host, sessionId, selected, onUpdateProgress)
                    PrerequisiteSetting.instance(host).introShown = true // no more intro if imported
                    true
                } catch (e: Exception) {
                    warning(host, TAG, host.getString(R.string.failed), e)
                    false
                } finally {
                    terminate()
                }
            withContext(Dispatchers.Main) {
                processDialog.dismiss()
                AlertDialog.Builder(host)
                    .setTitle(R.string.label_backup)
                    .setMessage(host.getString(if (result) R.string.state_completed else R.string.failed))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        if (result) Reboot.reboot(host)
                    }
                    .create().tintButtons().show()

                this@BackupImportDialog.dismiss()
            }
        }
    }

    private fun terminate() = Backup.Import.endImportBackupFromArchive(sessionId)

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        terminate()
    }

    companion object {
        private const val TAG = "BackupImportDialog"
        private const val KEY_SESSION = "session"
        fun newInstance(sessionId: Long): BackupImportDialog =
            BackupImportDialog().apply {
                arguments = Bundle().apply {
                    putLong(KEY_SESSION, sessionId)
                }
            }
    }

}