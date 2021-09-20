package com.kabouzeid.gramophone.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.DialogFragment
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.model.Playlist
import com.kabouzeid.gramophone.util.PlaylistsUtil

// Todo Completed
/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class DeletePlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playlists: List<Playlist> = requireArguments().getParcelableArrayList("playlists")!!
        val title: Int = if (playlists.size > 1) {R.string.delete_playlists_title}
                            else {R.string.delete_playlist_title}
        val content: CharSequence = if (playlists.size > 1) {Html.fromHtml(getString(R.string.delete_x_playlists, playlists.size))}
                            else {Html.fromHtml(getString(R.string.delete_playlist_x, playlists[0].name))}

        val dialog = MaterialDialog(requireActivity())
            .title(title)
            .message(text = content)
            .positiveButton(R.string.delete_action) {
                PlaylistsUtil.deletePlaylists(requireActivity(), playlists)
            }
            .negativeButton(android.R.string.cancel) { dismiss() }
        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))

        return dialog
    }

    companion object {
        @JvmStatic
        fun create(playlists: List<Playlist>?): DeletePlaylistDialog {
            val dialog = DeletePlaylistDialog()
            val args = Bundle()
            args.putParcelableArrayList("playlists", ArrayList(playlists))
            dialog.arguments = args
            return dialog
        }
    }
}
