/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.util.shareFileIntent
import androidx.fragment.app.DialogFragment
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class SongShareDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val song: Song = requireArguments().getParcelable(KEY)!!
        val currentlyListening = getString(R.string.currently_listening_to_x_by_x, song.title, song.artistName)
        val options = arrayOf(getString(R.string.the_audio_file), currentlyListening)

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.what_do_you_want_to_share)
            .setSingleChoiceItems(options, -1) { dialog, index: Int ->
                dialog.dismiss()
                when (index) {
                    0 -> startActivity(Intent.createChooser(shareFileIntent((dialog as Dialog).context, song), null))
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
            .setPositiveButton(android.R.string.ok) { d, _ -> d.dismiss() }
            .create()
    }

    companion object {
        private const val KEY = "song"
        fun create(song: Song): SongShareDialog =
            SongShareDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY, song)
                }
            }
    }
}
