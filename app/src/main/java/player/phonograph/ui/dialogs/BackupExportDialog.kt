/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import lib.storage.launcher.ICreateFileStorageAccessible
import player.phonograph.R
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.backup.Backup
import player.phonograph.model.backup.BackupItem
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.dateTimeSuffixCompat
import player.phonograph.util.theme.tintButtons
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class BackupExportDialog : DialogFragment() {

    private lateinit var adapter: BackupChooserAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        // setup view
        val view = requireActivity().layoutInflater.inflate(R.layout.recycler_view_wrapped, null)
        val allBackupConfig = BackupItem.entries.filter { !it.deprecated }
        val enabledBackupConfig = BackupItem.entries.filter { !it.deprecated && it.enabledByDefault }
        adapter = BackupChooserAdapter(enabledBackupConfig, allBackupConfig).also { it.init() }
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        adapter.attachToRecyclerView(recyclerView)

        val activity = WeakReference(requireActivity())

        // dialog
        val dialog = MaterialDialog(requireActivity())
            .title(text = getString(R.string.action_export, getString(R.string.label_backup)))
            .customView(view = view, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) { dialog ->
                val selected = adapter.currentConfig
                val host = activity.get() ?: return@positiveButton
                if (selected.isEmpty()) return@positiveButton
                require(host is ICreateFileStorageAccessible)
                host.createFileStorageAccessDelegate.launch(
                    "phonograph_plus_backup_${dateTimeSuffixCompat(currentDate())}.zip"
                ) { uri ->
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
                                dialog.dismiss()
                                completeDialog(host, result).show()
                            }
                        }
                    }
                }
            }
            .negativeButton(android.R.string.cancel) { it.dismiss() }
            .tintButtons()

        return dialog
    }

    private fun completeDialog(context: Context, success: Boolean) =
        AlertDialog.Builder(context)
            .setTitle(R.string.label_backup)
            .setMessage(context.getString(if (success) R.string.state_completed else R.string.failed))
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .create().tintButtons()


    companion object {
        private const val TAG = "BackupExportDialog"
    }
}