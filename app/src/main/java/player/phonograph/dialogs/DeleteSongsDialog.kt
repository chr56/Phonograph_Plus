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
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import player.phonograph.R
import player.phonograph.mediastore.MediaStoreUtil
import player.phonograph.model.Song
import player.phonograph.util.StringUtil
import util.mdcolor.pref.ThemeColor

/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad), chr_56<modify>
 */
class DeleteSongsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val attachedActivity: Activity = requireActivity()
        val songs: List<Song> = requireArguments().getParcelableArrayList("songs")!!

        // extra permission check on R(11)
        val hasPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                requireActivity().checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, "No MANAGE_EXTERNAL_STORAGE Permission")
                false
            } else {
                true
            }

        val message = StringUtil.buildDeletionMessage(
            context = requireContext(),
            itemSize = songs.size,
            extraSuffix = if (!hasPermission) requireContext().getString(
                R.string.permission_manage_external_storage_denied
            ) else "",
            StringUtil.ItemGroup(
                resources.getQuantityString(R.plurals.item_songs, songs.size),
                songs.map { it.title }
            )
        )

        val dialog = MaterialDialog(attachedActivity)
            .title(R.string.delete_action)
            .message(text = message)
            .positiveButton(R.string.delete_action) {
                MediaStoreUtil.deleteSongs(attachedActivity, songs)
            }
            .negativeButton(android.R.string.cancel)
            .apply {
                // grant permission button for R
                if (!hasPermission) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        neutralButton(R.string.grant_permission) {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply { // todo
//                            data = Uri.parse("package:${context.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            Handler().postDelayed({ attachedActivity.startActivity(intent) }, 200)
                        }
                    }
                }

                // set button color
                val color = ThemeColor.accentColor(requireActivity())
                getActionButton(WhichButton.POSITIVE).updateTextColor(color)
                getActionButton(WhichButton.NEGATIVE).updateTextColor(color)
                getActionButton(WhichButton.NEUTRAL).updateTextColor(color)
            }

        return dialog
    }

    companion object {
        private const val TAG = "DeleteSongsDialog"

        fun create(songs: ArrayList<Song>): DeleteSongsDialog =
            DeleteSongsDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList("songs", songs)
                }
            }
    }
}
