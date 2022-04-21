package player.phonograph.dialogs

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import util.mdcolor.pref.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.mediastore.MediaStoreUtil
/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad), chr_56<modify>
 */
class DeleteSongsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val attachedActivity: Activity = requireActivity()
        val songs: List<Song> = requireArguments().getParcelableArrayList("songs")!!
        val titleRes: Int = if (songs.size > 1) { R.string.delete_songs_title } else { R.string.delete_song_title }
        val msg: StringBuffer = StringBuffer()

        msg.append(
            Html.fromHtml(
                resources.getQuantityString(R.plurals.msg_song_deletion_summary, songs.size, songs.size), Html.FROM_HTML_MODE_LEGACY
            )
        )
        songs.forEach { song ->
            msg.append(song.title).appendLine()
        }

        // extra permission check on R(11)
        var hasPermission: Boolean = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (attachedActivity.checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(context, R.string.permission_manage_external_storage_denied, Toast.LENGTH_SHORT).show()
                Log.w(TAG, "No MANAGE_EXTERNAL_STORAGE permission")

                hasPermission = false
                msg.appendLine().append(attachedActivity.resources.getString(R.string.permission_manage_external_storage_denied))
            }
        }

        val dialog = MaterialDialog(attachedActivity)
            .title(titleRes)
            .message(text = msg)
            .positiveButton(R.string.delete_action) {
                MediaStoreUtil.deleteSongs(attachedActivity, songs)
            }
            .negativeButton(android.R.string.cancel)
        // grant permission button for R
        if (!hasPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                dialog.neutralButton(R.string.grant_permission) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply { // todo
//                            data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    Handler().postDelayed({ attachedActivity.startActivity(intent) }, 200)
                }
            }
        }

        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEUTRAL).updateTextColor(ThemeColor.accentColor(requireActivity()))

        return dialog
    }

    companion object {
        private const val TAG = "DeleteSongsDialog"

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
