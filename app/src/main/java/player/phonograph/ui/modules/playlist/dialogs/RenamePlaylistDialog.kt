/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.playlist.Playlist
import player.phonograph.util.lifecycleScopeOrNewOne
import player.phonograph.util.parcelable
import player.phonograph.util.theme.tintButtons
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import kotlinx.coroutines.launch

class RenamePlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playlist = requireArguments().parcelable<Playlist>(PLAYLIST)!!
        return MaterialDialog(requireActivity())
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
            ) { dialog, charSequence ->
                val name: String = charSequence.toString().trim()
                if (name.isNotEmpty()) {
                    dialog.context.lifecycleScopeOrNewOne().launch {
                        val result = PlaylistProcessors.writer(playlist)!!.rename(dialog.context, name)
                        Toast.makeText(
                            dialog.context,
                            if (result) R.string.success else R.string.failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .tintButtons()
    }

    companion object {
        private const val PLAYLIST = "playlist"

        fun create(playlist: Playlist): RenamePlaylistDialog =
            RenamePlaylistDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(PLAYLIST, playlist)
                }
            }
    }
}
