package player.phonograph.dialogs

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import player.phonograph.model.BasePlaylist
import player.phonograph.util.SAFCallbackHandlerActivity
import util.phonograph.m3u.PlaylistsManager
import java.lang.StringBuilder

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class DeletePlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val attachedActivity: Activity = requireActivity()
        val basePlaylists: List<BasePlaylist> = requireArguments().getParcelableArrayList("playlists")!!
        val title: Int = if (basePlaylists.size > 1) { R.string.delete_playlists_title } else { R.string.delete_playlist_title }

        val msg = StringBuilder(
            Html.fromHtml(resources.getQuantityString(R.plurals.msg_playlist_deletion_summary, basePlaylists.size, basePlaylists.size), Html.FROM_HTML_MODE_LEGACY)
        )
        basePlaylists.forEach { playlist ->
            msg.append(playlist.name).appendLine()
        }

        var hasPermission = true
        // extra permission check on R(11)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (attachedActivity.checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                hasPermission = false
                msg.appendLine().append(attachedActivity.resources.getString(R.string.permission_manage_external_storage_denied))
                Toast.makeText(context, R.string.permission_manage_external_storage_denied, Toast.LENGTH_SHORT).show()
                Log.w(TAG, "No MANAGE_EXTERNAL_STORAGE permission")
            }
        }

        val dialog = MaterialDialog(requireActivity())
            .title(title)
            .message(text = msg)
            .negativeButton(android.R.string.cancel) { dismiss() }
            .positiveButton(R.string.delete_action) {
                PlaylistsManager(
                    attachedActivity,
                    if (attachedActivity is SAFCallbackHandlerActivity) attachedActivity else null
                ).deletePlaylistWithGuide(basePlaylists)
            }
            .also {
                // grant permission button for R
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasPermission) {
                    it.neutralButton(R.string.grant_permission) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${attachedActivity.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    }
                }
                // set button color
                it.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(attachedActivity))
                it.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(attachedActivity))
                it.getActionButton(WhichButton.NEUTRAL).updateTextColor(ThemeColor.accentColor(attachedActivity))
            }

        return dialog
    }

    companion object {
        private const val TAG = "DeletePlaylistDialog"
        @JvmStatic
        fun create(basePlaylists: List<BasePlaylist>): DeletePlaylistDialog {
            val dialog = DeletePlaylistDialog()
            val args = Bundle()
            args.putParcelableArrayList("playlists", ArrayList(basePlaylists))
            dialog.arguments = args
            return dialog
        }
    }
}
