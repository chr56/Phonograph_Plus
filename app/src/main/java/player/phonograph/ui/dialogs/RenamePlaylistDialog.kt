/*
 *  Copyright (c) 2022~2023 chr_56, Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.input.input
import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistEdit
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.util.parcelable
import player.phonograph.util.theme.accentColor
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import kotlinx.coroutines.launch

class RenamePlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playlist = requireArguments().parcelable<FilePlaylist>(PLAYLIST) ?: throw Exception()
        val dialog = MaterialDialog(requireActivity())
            .title(R.string.rename_playlist_title)
            .positiveButton(R.string.rename_action)
            .negativeButton(android.R.string.cancel)
            .input(
                inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PERSON_NAME or
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                hintRes = R.string.playlist_name_empty,
                prefill = playlist.name,
                allowEmpty = false
            ) { _, charSequence ->
                val name: String = charSequence.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        PlaylistEdit.renamePlaylist(requireContext(), playlist, name)
                    }
                }
            }.apply {
                // set button color
                val accentColor = accentColor()
                getActionButton(WhichButton.POSITIVE).updateTextColor(accentColor)
                getActionButton(WhichButton.NEGATIVE).updateTextColor(accentColor)
            }
        return dialog
    }

    companion object {
        private const val PLAYLIST = "playlist"

        fun create(filePlaylist: FilePlaylist): RenamePlaylistDialog =
            RenamePlaylistDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(PLAYLIST, filePlaylist)
                }
            }
    }
}
