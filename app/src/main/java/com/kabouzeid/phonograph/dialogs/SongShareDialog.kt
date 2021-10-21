package com.kabouzeid.phonograph.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.kabouzeid.phonograph.R
import com.kabouzeid.phonograph.model.Song
import com.kabouzeid.phonograph.util.MusicUtil

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class SongShareDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val song: Song = requireArguments().getParcelable("song")!!
        val currentlyListening = getString(R.string.currently_listening_to_x_by_x, song.title, song.artistName)
        val options = arrayListOf<String>(getString(R.string.the_audio_file), currentlyListening)
        return MaterialDialog(activity as Context)
            .title(R.string.what_do_you_want_to_share)
            // getString(R.string.the_audio_file), "\u201C" + currentlyListening + "\u201D"
            .listItemsSingleChoice(items = options, checkedColor = ThemeColor.accentColor(activity as Context)) { _: MaterialDialog, i: Int, _: CharSequence ->
                when (i) {
                    0 -> startActivity(Intent.createChooser(MusicUtil.createShareSongFileIntent(song, context), null))
                    1 -> requireActivity().startActivity(
                        Intent.createChooser(
                            Intent()
                                .setAction(Intent.ACTION_SEND)
                                .putExtra(Intent.EXTRA_TEXT, currentlyListening)
                                .setType("text/plain"),
                            null
                        )
                    )
                }
            }
    }

    companion object {
        @JvmStatic
        fun create(song: Song): SongShareDialog {
            val dialog = SongShareDialog()
            val args = Bundle()
            args.putParcelable("song", song)
            dialog.arguments = args
            return dialog
        }
    }
}
