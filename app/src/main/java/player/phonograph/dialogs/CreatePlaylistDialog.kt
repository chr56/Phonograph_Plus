package player.phonograph.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import util.mdcolor.pref.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.input.input
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.util.PlaylistsUtil
import player.phonograph.misc.SAFCallbackHandlerActivity
import util.phonograph.m3u.PlaylistsManager

/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
class CreatePlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val songs: List<Song>? = requireArguments().getParcelableArrayList(SONGS)
        val dialog = MaterialDialog(requireActivity())
            .title(R.string.new_playlist_title)
            .positiveButton(R.string.create_action)
            .negativeButton(android.R.string.cancel)
            .input(
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PERSON_NAME or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                hintRes = R.string.playlist_name_empty,
                waitForPositiveButton = true,
                allowEmpty = false
            ) { _, input ->
                val name = input.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.failed), Toast.LENGTH_SHORT).show()
                    return@input
                }
                if (!PlaylistsUtil.doesPlaylistExist(requireActivity(), name)) {
                    val activity = requireActivity()
                    if (activity is SAFCallbackHandlerActivity) {
                        PlaylistsManager(activity, activity).createPlaylist(name, songs)
                    } else {
                        PlaylistsManager(activity, null).createPlaylist(name, songs)
                    }
                } else {
                    Toast.makeText(requireContext(), requireActivity().resources.getString(R.string.playlist_exists, name), Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))

        return dialog
    }

    companion object {
        private const val SONGS = "songs"

        @JvmStatic
        fun create(songs: List<Song>): CreatePlaylistDialog {
            val dialog = CreatePlaylistDialog()
            val args = Bundle()
            args.putParcelableArrayList(SONGS, ArrayList(songs))
            dialog.arguments = args
            return dialog
        }
        @JvmStatic
        fun createEmpty(): CreatePlaylistDialog {
            val dialog = CreatePlaylistDialog()
            val args = Bundle()
            args.putParcelableArrayList(SONGS, null)
            dialog.arguments = args
            return dialog
        }
    }
}
