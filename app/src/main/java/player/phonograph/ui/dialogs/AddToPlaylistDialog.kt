/*
 * Copyright (c) 2022~2023 chr_56, Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */

package player.phonograph.ui.dialogs

import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistEdit
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.util.parcelableArrayList
import player.phonograph.util.theme.tintButtons
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import android.app.Dialog
import android.os.Bundle
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AddToPlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val songs = requireArguments().parcelableArrayList<Song>(SONG)!!
        val playlists = runBlocking { PlaylistLoader.all(requireContext()) }

        val playlistNames =
            arrayOf(resources.getString(R.string.action_new_playlist)) + playlists.map { it.name }.toTypedArray()

        val fragmentActivity = requireActivity()

        return AlertDialog.Builder(fragmentActivity)
            .setTitle(R.string.add_playlist_title)
            .setItems(playlistNames) { dialog, index ->
                dialog.dismiss()
                fragmentActivity.lifecycleScope.launch {
                    if (index == 0) {
                        CreatePlaylistDialog.create(songs)
                            .show(fragmentActivity.supportFragmentManager, "ADD_TO_PLAYLIST")
                    } else {
                        PlaylistEdit.append(
                            context = fragmentActivity,
                            songs = songs,
                            filePlaylist = playlists[index - 1]
                        )
                    }
                }
            }
            .create().tintButtons()
    }

    companion object {
        fun create(songs: List<Song>): AddToPlaylistDialog =
            AddToPlaylistDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(SONG, ArrayList(songs))
                }
            }

        const val SONG: String = "songs"
    }
}
