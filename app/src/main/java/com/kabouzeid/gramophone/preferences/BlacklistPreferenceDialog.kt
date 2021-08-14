package com.kabouzeid.gramophone.preferences

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.dialogs.BlacklistFolderChooserDialog
import com.kabouzeid.gramophone.dialogs.BlacklistFolderChooserDialog.Companion.create
import com.kabouzeid.gramophone.dialogs.BlacklistFolderChooserDialog.FolderCallback
import com.kabouzeid.gramophone.provider.BlacklistStore
import java.io.File

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class BlacklistPreferenceDialog : DialogFragment(), FolderCallback {
    private lateinit var paths: List<String>
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val blacklistFolderChooserDialog = childFragmentManager.findFragmentByTag("FOLDER_CHOOSER") as BlacklistFolderChooserDialog?
        blacklistFolderChooserDialog?.setCallback(this)
        paths = BlacklistStore.getInstance(requireContext()).paths
        return MaterialDialog(requireContext())
            .title(R.string.blacklist)
            .positiveButton(android.R.string.ok) { dismiss() }
            .neutralButton(R.string.clear_action) {
                MaterialDialog(requireContext())
                    .title(R.string.clear_blacklist)
                    .message(R.string.do_you_want_to_clear_the_blacklist)
                    .positiveButton(R.string.clear_action) {
                        BlacklistStore.getInstance(requireContext()).clear()
                        refreshBlacklistData()
                    }
                    .negativeButton(android.R.string.cancel)
                    .show()
            }
            .negativeButton(R.string.add_action) {
                val dialog = create()
                dialog.setCallback(this@BlacklistPreferenceDialog)
                dialog.show(childFragmentManager, "FOLDER_CHOOSER")
            }
            .listItems(items = paths) { _, _, charSequence ->
                MaterialDialog(requireContext())
                    .title(R.string.remove_from_blacklist)
                    .message(text = Html.fromHtml(getString(R.string.do_you_want_to_remove_from_the_blacklist, charSequence)))
                    .positiveButton(R.string.remove_action) {
                        BlacklistStore.getInstance(requireContext()).removePath(File(charSequence.toString()))
                        refreshBlacklistData()
                    }
                    .negativeButton(android.R.string.cancel)
                    .show()
            }
            .noAutoDismiss()
    }

    @SuppressLint("CheckResult")
    private fun refreshBlacklistData() {
        val paths = BlacklistStore.getInstance(requireContext()).paths
        val dialog = dialog as MaterialDialog
        dialog.listItems(items = paths as List<CharSequence>)
    }

    override fun onFolderSelection(dialog: BlacklistFolderChooserDialog, file: File) {
        BlacklistStore.getInstance(requireContext()).addPath(file)
        refreshBlacklistData()
    }

    companion object {
        @JvmStatic
        fun newInstance(): BlacklistPreferenceDialog {
            return BlacklistPreferenceDialog()
        }
    }
}
