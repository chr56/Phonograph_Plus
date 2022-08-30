package player.phonograph.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import legacy.phonograph.LegacyPlaylistsUtil
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.model.PlaylistSong
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.StringUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class RemoveFromPlaylistDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val res = requireActivity().resources

        val songs: List<PlaylistSong> = requireArguments().getParcelableArrayList("songs")!!
        val playlist = PlaylistsUtil.getPlaylist(
            requireContext(),
            playlistId = songs.firstOrNull()?.playlistId ?: 0
        )

        val message = StringUtil.buildRemovalMessage(
            context = requireContext(),
            itemSize = songs.size,
            where = playlist.name,
            null,
            StringUtil.ItemGroup(
                res.getQuantityString(R.plurals.item_songs, songs.size),
                songs.map { it.title }
            )
        )

        return MaterialDialog(requireActivity())
            .title(R.string.remove_action)
            .message(text = message)
            .negativeButton(android.R.string.cancel)
            .positiveButton(R.string.remove_action) {
                if (activity != null) {
                    LegacyPlaylistsUtil.removeFromPlaylist(requireActivity(), songs)
                }
            }
            .apply {
                val color = ThemeColor.accentColor(requireActivity())
                // set button color
                getActionButton(WhichButton.POSITIVE).updateTextColor(color)
                getActionButton(WhichButton.NEGATIVE).updateTextColor(color)
            }
    }

    companion object {

        fun create(songs: List<PlaylistSong>): RemoveFromPlaylistDialog =
            RemoveFromPlaylistDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList("songs", ArrayList(songs))
                }
            }
    }
}
