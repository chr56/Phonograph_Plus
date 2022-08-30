package player.phonograph.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import mt.pref.ThemeColor.accentColor
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import java.io.File

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
            .listItemsSingleChoice(items = options, checkedColor = accentColor(requireContext())) { _: MaterialDialog, i: Int, _: CharSequence ->
                when (i) {
                    0 -> startActivity(Intent.createChooser(createShareSongFileIntent(song, requireContext()), null))
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
        internal fun createShareSongFileIntent(song: Song, context: Context): Intent {
            return try {
                Intent()
                    .setAction(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, context.applicationContext.packageName, File(song.data)))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setType("audio/*")
            } catch (e: IllegalArgumentException) {
                // the path is most likely not like /storage/emulated/0/... but something like /storage/28C7-75B0/...
                ErrorNotification.postErrorNotification(e, "Physical external SD card is not fully support!")
                Intent()
            }
        }
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
