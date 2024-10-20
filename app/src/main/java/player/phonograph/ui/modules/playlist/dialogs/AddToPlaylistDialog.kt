/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.util.parcelableArrayList
import player.phonograph.util.theme.tintButtons
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import android.app.Dialog
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddToPlaylistDialog : DialogFragment() {

    private lateinit var songs: List<Song>
    private lateinit var playlists: List<Playlist>
    private lateinit var playlistNames: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        songs = requireArguments().parcelableArrayList<Song>(SONG)!!
        playlists = requireArguments().parcelableArrayList<Playlist>(ALL_PLAYLISTS)!!
        playlistNames =
            arrayOf(resources.getString(R.string.action_new_playlist)) + playlists.map { it.name }.toTypedArray()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val fragmentActivity = requireActivity()
        return AlertDialog.Builder(fragmentActivity)
            .setTitle(R.string.add_playlist_title)
            .setItems(playlistNames) { dialog, index ->
                dialog.dismiss()
                if (index == 0) {
                    CreatePlaylistDialog.create(songs).show(fragmentActivity.supportFragmentManager, "ADD_TO_PLAYLIST")
                } else {
                    val targetPlaylist = playlists[index - 1]
                    fragmentActivity.lifecycleScope.launch(Dispatchers.IO) {
                        PlaylistProcessors.writer(targetPlaylist)!!.appendSongs(fragmentActivity, songs)
                    }
                }
            }
            .create().tintButtons()
    }

    companion object {
        fun create(songs: List<Song>, supportedPlaylist: List<Playlist>): AddToPlaylistDialog =
            AddToPlaylistDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(SONG, ArrayList(songs))
                    putParcelableArrayList(ALL_PLAYLISTS, ArrayList(supportedPlaylist))
                }
            }

        const val SONG: String = "songs"

        private const val ALL_PLAYLISTS: String = "playlists"
    }
}
