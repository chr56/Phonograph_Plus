package com.kabouzeid.gramophone.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.kabouzeid.gramophone.model.lyrics.AbsLyrics

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class LyricsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog(activity as Context)
                .title(text = requireArguments().getString("title")!!)
                .message(text = requireArguments().getString("lyrics")!!)
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