/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import lib.phonograph.misc.ICreateFileStorageAccess
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.adapter.sortable.BackupChooserAdapter
import player.phonograph.mechanism.backup.ALL_BACKUP_CONFIG
import player.phonograph.mechanism.backup.Backup
import player.phonograph.mechanism.backup.ENABLE_BACKUP_CONFIG
import player.phonograph.util.reportError
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.currentTimestamp
import player.phonograph.util.text.dateTimeSuffixCompat
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
        adapter = BackupChooserAdapter(ENABLE_BACKUP_CONFIG, ALL_BACKUP_CONFIG).also { it.init() }
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        adapter.attachToRecyclerView(recyclerView)

        val activity = WeakReference(requireActivity())

        // dialog
        val dialog = MaterialDialog(requireActivity())
            .title(text = getString(R.string.action_import, getString(R.string.action_backup)))
            .customView(view = view, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) { dialog ->
                val selected = adapter.currentConfig
                val host = activity.get() ?: return@positiveButton
                require(host is ICreateFileStorageAccess)
                host.createFileStorageAccessTool.launch(
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
                                    reportError(e, TAG, host.getString(R.string.failed))
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
            .apply {
                val color = ThemeColor.accentColor(requireActivity())
                getActionButton(WhichButton.POSITIVE).updateTextColor(color)
                getActionButton(WhichButton.NEGATIVE).updateTextColor(color)
            }

        return dialog
    }

    private fun completeDialog(context: Context, success: Boolean) =
        AlertDialog.Builder(context)
            .setTitle(R.string.action_backup)
            .setMessage(context.getString(if (success) R.string.completed else R.string.failed))
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .create()


    companion object {
        private const val TAG = "BackupExportDialog"
    }
}