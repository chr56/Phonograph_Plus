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
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import player.phonograph.R
import player.phonograph.model.Playlist
import player.phonograph.util.MediaStoreUtil
import player.phonograph.util.SAFCallbackHandlerActivity
import player.phonograph.util.Util

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
            .positiveButton(R.string.delete_action) {
                val activity = requireActivity()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    if (activity is SAFCallbackHandlerActivity) {
                        val defaultLocation = activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                        val initialUri = Uri.fromFile(defaultLocation)

                        val deleteList: MutableList<DocumentFile> = ArrayList()

                        activity.getSafLauncher().openDir(initialUri) { uri: Uri? ->
                            if (uri != null) {
                                val documentDir = DocumentFile.fromTreeUri(activity, uri)
                                documentDir?.let { dir ->

                                    dir.listFiles().forEach { file ->
                                        if (file.isFile) {
                                            playlists.forEach { playlist ->
                                                if (file.name != null)
                                                    if (playlist.name == file.name!!.dropLastWhile { it != '.' }.dropLast(1)) {
                                                        deleteList.add(file)
                                                    }
                                            }
                                        }
                                    }

                                    if (deleteList.isNotEmpty()) {
                                        val msg = StringBuffer()
                                            .append(Html.fromHtml(activity.resources.getQuantityString(R.plurals.msg_playlist_deletion_summary, deleteList.size, deleteList.size), Html.FROM_HTML_MODE_LEGACY))
                                        deleteList.forEach { file ->
                                            Log.d("FILE_DEL", "DEL: ${file.name}@${file.uri}")
                                            val name = file.uri.path.toString().substringAfter(":").substringAfter(":")
                                            msg.append(name).appendLine()
                                        }

                                        MaterialDialog(activity)
                                            .title(R.string.delete_playlist_title)
                                            .message(text = msg)
                                            .positiveButton(R.string.delete_action) {
                                                deleteList.forEach { it.delete() }
                                                Util.sentPlaylistChangedLocalBoardCast()
                                            }
                                            .negativeButton(android.R.string.cancel) { it.dismiss() }
                                            .show()
                                    } else {
                                        MaterialDialog(activity)
                                            .title(R.string.delete_playlist_title)
                                            .message(R.string.failed_to_delete)
                                            .show()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    MediaStoreUtil.deletePlaylists(requireActivity(), playlists)
                }
            }
            .negativeButton(android.R.string.cancel) { dismiss() }
        // grant permission button for R
        if (!hasPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                dialog.neutralButton(R.string.grant_permission) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${attachedActivity.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
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
