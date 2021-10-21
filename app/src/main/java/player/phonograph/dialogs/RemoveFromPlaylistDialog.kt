package player.phonograph.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.DialogFragment
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import player.phonograph.R
import player.phonograph.model.PlaylistSong
import player.phonograph.util.PlaylistsUtil
import java.util.*

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class RemoveFromPlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val songs: List<PlaylistSong> = requireArguments().getParcelableArrayList("songs")!!
        val title: Int = if (songs.size > 1) { R.string.remove_songs_from_playlist_title } else { R.string.remove_song_from_playlist_title }
        val content: CharSequence
        val msg: StringBuffer = StringBuffer()

        msg.append(Html.fromHtml(
            resources.getQuantityString(R.plurals.msg_song_removal_summary, songs.size, songs.size)
            , Html.FROM_HTML_MODE_LEGACY)
        )
        songs.forEach{ song ->
            msg.append(song.title).appendLine()
        }
        val dialog = MaterialDialog(requireActivity())
            .title(title)
            .message(text = msg)
            .negativeButton(android.R.string.cancel)
            .positiveButton(R.string.remove_action) { if (activity != null) PlaylistsUtil.removeFromPlaylist(requireActivity(), songs) }
        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        return dialog
    }

    companion object {

        @JvmStatic
        fun create(song: PlaylistSong): RemoveFromPlaylistDialog {
            val list: MutableList<PlaylistSong> = ArrayList()
            list.add(song)
            return create(list)
        }

        @JvmStatic
        fun create(songs: List<PlaylistSong>): RemoveFromPlaylistDialog {
            val dialog = RemoveFromPlaylistDialog()
            val args = Bundle()
            args.putParcelableArrayList("songs", ArrayList(songs))
            dialog.arguments = args
            return dialog
        }
    }
}
