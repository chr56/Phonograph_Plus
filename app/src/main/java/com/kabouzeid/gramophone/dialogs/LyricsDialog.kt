package com.kabouzeid.gramophone.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.kabouzeid.gramophone.model.lyrics.AbsLyrics
import java.lang.StringBuilder
import java.util.regex.Pattern

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class LyricsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val rawText: Array<String> = requireArguments().getString("lyrics")!!.split(Pattern.compile("\\\\[nNrR]")).toTypedArray()
        val builder = StringBuilder()
        for (s in rawText) {
            builder.append(s).append("\n")
        }
        val text = builder.toString()

        return MaterialDialog(activity as Context)
                .title(text = requireArguments().getString("title")!!)
                .message(text = text)
    }

    companion object {
//        @JvmStatic
//        fun create(lyrics: Lyrics): LyricsDialog {
//            val dialog = LyricsDialog()
//            val args = Bundle()
//            args.putString("title", lyrics.song.title)
//            args.putString("lyrics", lyrics.text)
//            dialog.arguments = args
//            return dialog
//        }
        @JvmStatic
        fun create(lyrics: AbsLyrics): LyricsDialog {
            val dialog = LyricsDialog()
            val args = Bundle()
            args.putString("title", lyrics.getTitle() as String)
            args.putString("lyrics", lyrics.getText())
            dialog.arguments = args
            return dialog
        }
    }
}