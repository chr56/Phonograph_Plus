package com.kabouzeid.gramophone.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.model.smartplaylist.AbsSmartPlaylist
// Todo Completed
/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class ClearSmartPlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playlist: AbsSmartPlaylist = requireArguments().getParcelable("playlist")!!
        val title = R.string.clear_playlist_title
        val content: CharSequence = Html.fromHtml(getString(R.string.clear_playlist_x, playlist.name))
        return MaterialDialog(requireActivity())
            .title(title)
            .message(text = content)
            .negativeButton(android.R.string.cancel)
            .positiveButton(R.string.clear_action) {
                if (activity != null) {
                    playlist.clear(activity)
                }
            }
    }
    companion object {
        @JvmStatic
        fun create(playlist: AbsSmartPlaylist?): ClearSmartPlaylistDialog {
            val dialog = ClearSmartPlaylistDialog()
            val args = Bundle()
            args.putParcelable("playlist", playlist)
            dialog.arguments = args
            return dialog
        }
    }
}
