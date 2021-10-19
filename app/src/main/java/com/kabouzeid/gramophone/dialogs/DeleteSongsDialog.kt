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
import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.util.MediaStoreUtil
// Todo Completed Review
/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad), chr_56<modify>
 */
class DeleteSongsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val songs: List<Song> = requireArguments().getParcelableArrayList("songs")!!
        val titleRes: Int = if (songs.size > 1) { R.string.delete_songs_title } else { R.string.delete_song_title }
        val msg: StringBuffer = StringBuffer()

        msg.append(Html.fromHtml(
            resources.getQuantityString(R.plurals.msg_song_deletion_summary, songs.size, songs.size)
            , Html.FROM_HTML_MODE_LEGACY)
        )
        songs.forEach{ song ->
            msg.append(song.title).appendLine()
        }

        val dialog = MaterialDialog(requireActivity())
            .title(titleRes)
            .message(text = msg)
            .positiveButton(R.string.delete_action) {
                MediaStoreUtil.deleteSongs(requireActivity(), songs)
            }
            .negativeButton(android.R.string.cancel)
        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))

        return dialog
    }

    companion object {
        @JvmStatic
        fun create(songs: List<Song>): DeleteSongsDialog {
            val dialog = DeleteSongsDialog()
            val args = Bundle()
            args.putParcelableArrayList("songs", ArrayList(songs))
            dialog.arguments = args
            return dialog
        }
    }
}
