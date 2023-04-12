/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.adapter.sortable.BackupChooserAdapter
import player.phonograph.migrate.backup.Backup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.Dialog
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class BackupImportDialog : DialogFragment() {

    private var sessionId: Long = 0

    private lateinit var adapter: BackupChooserAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        sessionId = arguments?.getLong(KEY_SESSION) ?: throw IllegalArgumentException("No session id!")

        // read manifest
        val manifest = Backup.readManifest(sessionId) ?: throw IllegalArgumentException("No Manifest found!")
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
                dialog.dismiss()
                lifecycleScope.launch(Dispatchers.IO) {
                    Backup.executeImport(host, sessionId, selected)
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

    override fun onDestroy() {
        super.onDestroy()
        Backup.endImportBackupFromArchive(sessionId)
    }

    companion object {
        private const val KEY_SESSION = "session"
        fun newInstance(sessionId: Long): BackupImportDialog =
            BackupImportDialog().apply {
                arguments = Bundle().apply {
                    putLong(KEY_SESSION, sessionId)
                }
            }
    }
}