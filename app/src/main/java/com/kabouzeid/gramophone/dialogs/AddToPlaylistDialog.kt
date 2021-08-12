package com.kabouzeid.gramophone.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.loader.PlaylistLoader
import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.util.PlaylistsUtil
// import java.util.*

/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
class AddToPlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playlists = PlaylistLoader.getAllPlaylists(requireActivity())
        val playlistNames = arrayOfNulls<CharSequence>(playlists.size + 1)
        playlistNames[0] = requireActivity().resources.getString(R.string.action_new_playlist)
        for (i in 1 until playlistNames.size) {
            playlistNames[i] = playlists[i - 1].name
        }
        return MaterialDialog(requireActivity())
            .title(R.string.add_playlist_title)
            .listItems(items = playlistNames as List<CharSequence>/*TODO*/) { materialDialog: MaterialDialog, i: Int, _: CharSequence ->
                val songs: List<Song> = requireArguments().getParcelableArrayList("songs")
                    ?: return@listItems
                if (i == 0) {
                    materialDialog.dismiss()
                    CreatePlaylistDialog.create(songs).show(requireActivity().supportFragmentManager, "ADD_TO_PLAYLIST")
                } else {
                    materialDialog.dismiss()
                    PlaylistsUtil.addToPlaylist(requireActivity(), songs, playlists[i - 1].id, true)
                }
            }
    }

    companion object {
        fun create(song: Song): AddToPlaylistDialog {
            val list: MutableList<Song> = ArrayList()
            list.add(song)
            return create(list)
        }

        @JvmStatic
        fun create(songs: List<Song>?): AddToPlaylistDialog {
            val dialog = AddToPlaylistDialog()
            val args = Bundle()
            args.putParcelableArrayList("songs", ArrayList(songs))
            dialog.arguments = args
            return dialog
        }
    }
}
