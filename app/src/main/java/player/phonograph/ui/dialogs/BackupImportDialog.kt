/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import lib.phonograph.misc.Reboot
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.adapter.sortable.BackupChooserAdapter
import player.phonograph.migrate.backup.Backup
import player.phonograph.util.reportError
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.ProgressBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class BackupImportDialog : DialogFragment() {

    private var sessionId: Long = 0

    private lateinit var adapter: BackupChooserAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        sessionId = arguments?.getLong(KEY_SESSION) ?: throw IllegalArgumentException("No session id!")

        // read manifest
        val manifest = Backup.Import.readManifest(sessionId) ?: throw IllegalArgumentException("No Manifest found!")
        val contained = manifest.files.map { it.key }

        // setup view
        val view = requireActivity().layoutInflater.inflate(R.layout.recycler_view_wrapped, null)
        adapter = BackupChooserAdapter(contained).also { it.init() }
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
                val processDialog = progressDialog(host)
                dialog.dismiss()
                processDialog.show()
                lifecycleScope.launch(Dispatchers.IO) {
                    val result =
                        try {
                            Backup.Import.executeImport(host, sessionId, selected)
                            true
                        } catch (e: Exception) {
                            reportError(e, TAG, host.getString(R.string.failed))
                            false
                        } finally {
                            terminateBackup()
                        }
                    withContext(Dispatchers.Main) {
                        processDialog.dismiss()
                        completeDialog(host, result).show()
                    }
                }
            }
            .negativeButton(android.R.string.cancel) {
                terminateBackup()
                it.dismiss()
            }
            .apply {
                val color = ThemeColor.accentColor(requireActivity())
                getActionButton(WhichButton.POSITIVE).updateTextColor(color)
                getActionButton(WhichButton.NEGATIVE).updateTextColor(color)
            }

        return dialog
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

    private fun progressDialog(context: Context) =
        AlertDialog.Builder(context)
            .setTitle(R.string.action_backup)
            .setView(
                ProgressBar(context).also {
                    it.isIndeterminate = true
                    it.setPadding(32)
                }
            )
            .setCancelable(false)
            .create()

    private fun completeDialog(context: Context, success: Boolean) =
        AlertDialog.Builder(context)
            .setTitle(R.string.action_backup)
            .setMessage(context.getString(if (success) R.string.completed else R.string.failed))
            .setPositiveButton(context.getString(R.string.action_reboot)) { _, _ ->
                Reboot.reboot(context)
            }
            .create()

    private fun terminateBackup() = Backup.Import.endImportBackupFromArchive(sessionId)
}