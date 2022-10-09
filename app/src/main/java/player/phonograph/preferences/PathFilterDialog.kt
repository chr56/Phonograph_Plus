package player.phonograph.preferences

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.list.listItems
import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.ui.dialogs.PathFilterFolderChooserDialog
import player.phonograph.provider.PathFilterStore
import java.io.File

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class PathFilterDialog : DialogFragment() {
    private lateinit var paths: List<String>
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        paths = PathFilterStore.getInstance(requireContext()).blacklistPaths
        val dialog = MaterialDialog(requireContext())
            .title(R.string.blacklist)
            .positiveButton(android.R.string.ok) { dismiss() }
            .neutralButton(R.string.clear_action) {
                MaterialDialog(requireContext())
                    .title(R.string.clear_blacklist)
                    .message(R.string.do_you_want_to_clear_the_blacklist)
                    .positiveButton(R.string.clear_action) {
                        PathFilterStore.getInstance(App.instance).clearBlacklist()
                        refreshBlacklistData()
                    }
                    .negativeButton(android.R.string.cancel)
                    .show()
            }
            .negativeButton(R.string.add_action) {
                PathFilterFolderChooserDialog().show(parentFragmentManager, "FOLDER_CHOOSER")
                dismiss()
            }
            .listItems(items = paths, waitForPositiveButton = false) { _, _, charSequence ->
                MaterialDialog(requireContext())
                    .title(R.string.remove_from_blacklist)
                    .message(text = Html.fromHtml(getString(R.string.do_you_want_to_remove_from_the_blacklist, charSequence)))
                    .positiveButton(R.string.remove_action) {
                        PathFilterStore.getInstance(App.instance).removeBlacklistPath(File(charSequence.toString()))
                        refreshBlacklistData()
                    }
                    .negativeButton(android.R.string.cancel)
                    .show()
            }
            .noAutoDismiss()
        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEUTRAL).updateTextColor(ThemeColor.accentColor(requireActivity()))
        return dialog
    }

    @SuppressLint("CheckResult")
    private fun refreshBlacklistData() {
        val paths = PathFilterStore.getInstance(App.instance).blacklistPaths
        val dialog = dialog as MaterialDialog
        dialog.listItems(items = paths)
    }

    companion object {
        fun newInstance(): PathFilterDialog {
            return PathFilterDialog()
        }
    }
}
