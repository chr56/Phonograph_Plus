/*
 *  Copyright (c) 2022~2023 chr_56, Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.input.input
import mt.pref.ThemeColor.accentColor
import player.phonograph.R
import player.phonograph.mechanism.PlaylistsManagement
import util.phonograph.playlist.mediastore.renamePlaylistViaMediastore
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import kotlinx.coroutines.launch

class RenamePlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playlistId = requireArguments().getLong(PLAYLIST_ID)
        val dialog = MaterialDialog(requireActivity())
            .title(R.string.rename_playlist_title)
            .positiveButton(R.string.rename_action)
            .negativeButton(android.R.string.cancel)
            .input(
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PERSON_NAME or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                hintRes = R.string.playlist_name_empty,
                prefill = PlaylistsManagement.getNameForPlaylist(requireActivity(), playlistId),
                allowEmpty = false
            ) { _, charSequence ->
                val name: String = charSequence.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        renamePlaylistViaMediastore(requireActivity(), requireArguments().getLong(PLAYLIST_ID), name)
                    }
                }
            }.apply {
                // set button color
                getActionButton(WhichButton.POSITIVE).updateTextColor(accentColor(requireActivity()))
                getActionButton(WhichButton.NEGATIVE).updateTextColor(accentColor(requireActivity()))
            }
        return dialog
    }

    companion object {
        private const val PLAYLIST_ID = "playlist_id"

        fun create(playlistId: Long): RenamePlaylistDialog =
            RenamePlaylistDialog().apply {
                arguments = Bundle().apply {
                    putLong(PLAYLIST_ID, playlistId)
                }
            }
    }
}
