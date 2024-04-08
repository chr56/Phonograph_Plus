/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.file.Location
import player.phonograph.repo.database.PathFilterStore
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.view.View
import java.io.File

class PathFilterFolderChooserDialog : FileChooserDialog() {

    private val mode get() = Setting(App.instance)[Keys.pathFilterExcludeMode].data

    override fun affirmative(view: View, currentLocation: Location) {
        val file = File(currentLocation.absolutePath)
        MaterialDialog(requireContext())
            .title(
                text = getString(
                    R.string.path_filter_confirmation, getString(if (mode) R.string.excluded_paths else R.string.included_paths)
                )
            )
            .message(text = file.absolutePath)
            .positiveButton(android.R.string.ok) {
                with(PathFilterStore.get()) {
                    if (mode) addBlacklistPath(file) else addWhitelistPath(file)
                }

                it.dismiss() // dismiss this alert dialog
                this.dismiss() // dismiss Folder Chooser

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
