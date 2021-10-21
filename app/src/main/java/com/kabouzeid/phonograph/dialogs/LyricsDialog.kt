package com.kabouzeid.phonograph.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.kabouzeid.phonograph.R
import com.kabouzeid.phonograph.adapter.LyricsAdapter
import com.kabouzeid.phonograph.model.Song
import com.kabouzeid.phonograph.model.lyrics.AbsLyrics

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class LyricsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val lines: Array<CharSequence> = requireArguments().getCharSequenceArray(LINE_ARRAY)!!
        val timeStamps: IntArray = requireArguments().getIntArray(TIME_ARRAY)!!
        var title: String = requireArguments().getString(TITTLE)!!
        if (title.equals(AbsLyrics.DEFAULT_TITLE)) title = requireArguments().getString(SONG)!!

        val dialog = MaterialDialog(activity as Context)
            .title(text = title)
            .positiveButton { dismiss() }
            .customView(R.layout.dialog_lyrics, horizontalPadding = true)
        val recyclerView = dialog.getCustomView().findViewById<RecyclerView>(R.id.recycler_view_lyrics)
        recyclerView.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        recyclerView.adapter = LyricsAdapter(timeStamps, lines)

        return dialog
    }

    companion object {
        private const val LINE_ARRAY = "lyrics lines array"
        private const val TIME_ARRAY = "lyrics time stamp array"
        private const val TITTLE = "title"
        private const val SONG = "song"

        @JvmStatic
        fun create(lyrics: AbsLyrics, song: Song): LyricsDialog {
            val dialog = LyricsDialog()
            val args = Bundle()
            args.putString(TITTLE, lyrics.getTitle() as String)
            args.putString(SONG, song.title)
            args.putIntArray(TIME_ARRAY, lyrics.getLyricsTimeArray())
            args.putCharSequenceArray(LINE_ARRAY, lyrics.getLyricsLineArray())
            dialog.arguments = args
            return dialog
        }
    }
}
