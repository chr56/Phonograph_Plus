package player.phonograph.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import player.phonograph.R
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.model.playlist.ResettablePlaylist
import util.mdcolor.pref.ThemeColor
/**
 * @author Karim Abou Zeid (kabouzeid)
 */
// todo rename
class ClearSmartPlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playlist: SmartPlaylist = requireArguments().getParcelable("playlist")!!
        val title = R.string.clear_playlist_title
        val content: CharSequence = Html.fromHtml(getString(R.string.clear_playlist_x, playlist.name))
        val dialog = MaterialDialog(requireActivity())
            .title(title)
            .message(text = content)
            .negativeButton(android.R.string.cancel)
            .positiveButton(R.string.clear_action) {
                if (playlist is ResettablePlaylist) {
                    playlist.clear(requireActivity())
                } else {
                    Toast.makeText(requireActivity(), "Unclearable!!", Toast.LENGTH_SHORT).show() // todo string
                }
            }
        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        return dialog
    }
    companion object {
        @JvmStatic
        fun create(playlist: SmartPlaylist): ClearSmartPlaylistDialog {
            val dialog = ClearSmartPlaylistDialog()
            val args = Bundle()
            args.putParcelable("playlist", playlist)
            dialog.arguments = args
            return dialog
        }
    }
}
