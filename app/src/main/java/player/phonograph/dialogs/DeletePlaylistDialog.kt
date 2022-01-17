package player.phonograph.dialogs

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import player.phonograph.R
import player.phonograph.model.Playlist
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.SAFCallbackHandlerActivity

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class DeletePlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val attachedActivity: Activity = requireActivity()
        val playlists: List<Playlist> = requireArguments().getParcelableArrayList("playlists")!!
        val title: Int = if (playlists.size > 1) { R.string.delete_playlists_title } else { R.string.delete_playlist_title }

        val msg: StringBuffer = StringBuffer()
        msg.append(
            Html.fromHtml(
                resources.getQuantityString(R.plurals.msg_playlist_deletion_summary, playlists.size, playlists.size), Html.FROM_HTML_MODE_LEGACY
            )
        )
        playlists.forEach { playlist ->
            msg.append(playlist.name).appendLine()
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

        val dialog = MaterialDialog(requireActivity())
            .title(title)
            .message(text = msg)
            .negativeButton(android.R.string.cancel) { dismiss() }
            .positiveButton(R.string.delete_action) {
                val activity = requireActivity()

                val failList = PlaylistsUtil.deletePlaylists(requireActivity(), playlists)
                if (failList.isNotEmpty()) {
                    val list = StringBuffer()
                    for (playlist in failList) {
                        list.append(playlist.name).append("\n")
                    }
                    // report failure
                    MaterialDialog(requireContext())
                        .title(R.string.failed_to_delete)
                        .message(
                            text = "${
                            requireActivity().resources.getQuantityString(R.plurals.msg_deletion_result, playlists.size, playlists.size - failList.size, playlists.size)
                            }\n ${requireActivity().getString(R.string.failed_to_delete)}: \n $list "
                        )
                        .positiveButton(android.R.string.ok)
                        // retry
                        .negativeButton(R.string.delete_with_saf) {
                            if (activity is SAFCallbackHandlerActivity) {
                                val defaultLocation = activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                                val initialUri = Uri.fromFile(defaultLocation)
                                Toast.makeText(activity, R.string.direction_open_folder_with_saf, Toast.LENGTH_SHORT).show()
                                activity.getSafLauncher().openDir(initialUri) { uri: Uri? ->
                                    uri?.let { PlaylistsUtil.deletePlaylistsInDir(activity, playlists, it) }
                                    return@openDir Unit
                                }
                            } else {
                                Toast.makeText(activity, R.string.failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                        .also {
                            // color
                            it.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
                            it.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
                            it.getActionButton(WhichButton.NEUTRAL).updateTextColor(ThemeColor.accentColor(requireActivity()))
                        }
                        .show()
                }
            }
        // grant permission button for R
        if (!hasPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                dialog.neutralButton(R.string.grant_permission) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${attachedActivity.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
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
        private const val TAG = "DeletePlaylistDialog"
        @JvmStatic
        fun create(playlists: List<Playlist>): DeletePlaylistDialog {
            val dialog = DeletePlaylistDialog()
            val args = Bundle()
            args.putParcelableArrayList("playlists", ArrayList(playlists))
            dialog.arguments = args
            return dialog
        }
    }
}

fun removeSuffix(s: String): String {
    var t = s
    while (!t.endsWith('.', true)) {
        t = t.dropLastWhile { it != '.' }.dropLast(1)
    }
    return t
}
