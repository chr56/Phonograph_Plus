package com.kabouzeid.gramophone.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.adapter.SimpleAdapter
import com.kabouzeid.gramophone.model.lyrics.AbsLyrics

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class LyricsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

//        val rawText: Array<String> = requireArguments().getString("lyrics text")!!.split(Pattern.compile("(\\\\[nNrR])|(\\r?\\n)")).toTypedArray()
        val lines: Array<CharSequence> = requireArguments().getCharSequenceArray(LINE_ARRAY)!!
        val timeStamps: IntArray = requireArguments().getIntArray(TIME_ARRAY)!!

//        val builder = StringBuilder()
//        for (s in rawText) {
//            builder.append(s).append("\n")
//        }
//        val text = builder.toString()

        val dialog = MaterialDialog(activity as Context)
            .title(text = requireArguments().getString(TITTLE)!!)
            .positiveButton { dismiss() }
            .customView(R.layout.dialog_lyrics, horizontalPadding = true)
        val recyclerView = dialog.getCustomView().findViewById<RecyclerView>(R.id.recycler_view_lyrics)
        recyclerView.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        recyclerView.adapter = SimpleAdapter(timeStamps, lines)
        recyclerView

        return dialog
    }

    companion object {
        private const val LINE_ARRAY = "lyrics lines array"
        private const val TIME_ARRAY = "lyrics time stamp array"
        private const val TITTLE = "title"

        @JvmStatic
        fun create(lyrics: AbsLyrics): LyricsDialog {
            val dialog = LyricsDialog()
            val args = Bundle()
            args.putString(TITTLE, lyrics.getTitle() as String)
//            args.putString("lyrics text", lyrics.getText())
            args.putIntArray(TIME_ARRAY, lyrics.getLyricsTimeArray())
            args.putCharSequenceArray(LINE_ARRAY, lyrics.getLyricsLineArray())
            dialog.arguments = args
            return dialog
        }
    }
}
