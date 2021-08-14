package com.kabouzeid.gramophone.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.model.PlaylistSong
import com.kabouzeid.gramophone.util.PlaylistsUtil
import java.util.*

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class RemoveFromPlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val songs: List<PlaylistSong> = requireArguments().getParcelableArrayList("songs")!!
        val title: Int
        val content: CharSequence
        if (songs.size > 1) {
            title = R.string.remove_songs_from_playlist_title
            content = Html.fromHtml(getString(R.string.remove_x_songs_from_playlist, songs.size))
        } else {
            title = R.string.remove_song_from_playlist_title
            content = Html.fromHtml(getString(R.string.remove_song_x_from_playlist, songs[0].title))
        }
        return MaterialDialog(requireActivity())
            .title(title)
            .message(text = content)
            .negativeButton(android.R.string.cancel)
            .positiveButton(R.string.remove_action) { if (activity != null) PlaylistsUtil.removeFromPlaylist(requireActivity(), songs) }
    }

    companion object {
        fun create(song: PlaylistSong): RemoveFromPlaylistDialog {
            val list: MutableList<PlaylistSong> = ArrayList()
            list.add(song)
            return create(list)
        }

        @JvmStatic
        fun create(songs: List<PlaylistSong>?): RemoveFromPlaylistDialog {
            val dialog = RemoveFromPlaylistDialog()
            val args = Bundle()
            args.putParcelableArrayList("songs", ArrayList(songs))
            dialog.arguments = args
            return dialog
        }
    }
}
