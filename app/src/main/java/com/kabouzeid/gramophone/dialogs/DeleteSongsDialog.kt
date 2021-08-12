package com.kabouzeid.gramophone.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.util.MusicUtil
import java.util.*
//Todo
/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
class DeleteSongsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val songs: List<Song>? = requireArguments().getParcelableArrayList("songs")!!
        val title: Int = if (songs?.size!! > 1){R.string.delete_songs_title}else{R.string.delete_song_title}
        val content: CharSequence = if (songs.size > 1) {
                Html.fromHtml(getString(R.string.delete_x_songs, songs?.size))
            } else {
                Html.fromHtml(getString(R.string.delete_song_x, songs?.get(0)?.title))
            }

        return MaterialDialog(requireActivity())
            .title(title)
            .message(text = content)
            .positiveButton(R.string.delete_action) {
                if (songs != null && requireActivity()!=null) {
                    MusicUtil.deleteTracks(requireActivity(), songs)
                }
            }
            .negativeButton(android.R.string.cancel)
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
            args.putParcelableArrayList("songs", ArrayList(songs as ArrayList<Song>) )
            dialog.arguments = args
            return dialog
        }
    }
}
