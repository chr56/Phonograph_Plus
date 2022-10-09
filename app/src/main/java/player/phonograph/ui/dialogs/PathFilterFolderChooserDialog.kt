package player.phonograph.ui.dialogs

import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.file.Location
import player.phonograph.preferences.PathFilterDialog
import player.phonograph.provider.PathFilterStore
import java.io.File

class PathFilterFolderChooserDialog : FileChooserDialog() {

    override fun affirmative(view: View, currentLocation: Location) {
        val file = File(currentLocation.absolutePath)
        MaterialDialog(requireContext())
            .title(R.string.add_blacklist)
            .message(text = file.absolutePath)
            .positiveButton(android.R.string.ok) {
                PathFilterStore.getInstance(App.instance).addBlacklistPath(file)

                it.dismiss() // dismiss this alert dialog
                this.dismiss() // dismiss Folder Chooser

                PathFilterDialog.newInstance()
                    .show(parentFragmentManager, "Blacklist_Preference_Dialog") // then reopen BlacklistPreferenceDialog
            }
            .negativeButton(android.R.string.cancel) {
                it.dismiss() // dismiss this alert dialog
            }
            .apply {
                getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
                getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
            }
            .show()
    }
}
