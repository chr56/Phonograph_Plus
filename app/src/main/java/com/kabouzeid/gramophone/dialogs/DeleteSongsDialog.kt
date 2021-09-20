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
import com.kabouzeid.gramophone.util.MusicUtil
// Todo Completed Review
/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad), chr_56<modify>
 */
class DeleteSongsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val songs: List<Song>? = requireArguments().getParcelableArrayList("songs")!!
        val titleRes: Int = if (songs?.size!! > 1) { R.string.delete_songs_title } else { R.string.delete_song_title }
        val content: CharSequence = if (songs.size > 1) {
            Html.fromHtml(getString(R.string.delete_x_songs, songs.size))
        } else {
            Html.fromHtml(getString(R.string.delete_song_x, songs[0].title))
        }

        val dialog = MaterialDialog(requireActivity())
            .title(titleRes)
            .message(text = content)
            .positiveButton(R.string.delete_action) {
                if (songs != null && requireActivity() != null) {
                    MusicUtil.deleteTracks(requireActivity(), songs)
                }
            }
            .negativeButton(android.R.string.cancel)
        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))

        return dialog
    }

    companion object {
        fun create(song: Song): DeleteSongsDialog {
            val list: MutableList<Song> = ArrayList()
            list.add(song)
            return create(list)
        }

        @JvmStatic
        fun create(songs: List<Song>?): DeleteSongsDialog {
            val dialog = DeleteSongsDialog()
            val args = Bundle()
            args.putParcelableArrayList("songs", ArrayList(songs))
            dialog.arguments = args
            return dialog
        }
    }
}
