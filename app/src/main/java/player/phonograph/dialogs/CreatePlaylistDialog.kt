package player.phonograph.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.input.input
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import player.phonograph.App
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED
import player.phonograph.R
import player.phonograph.helper.M3UWriter
import player.phonograph.model.Song
import player.phonograph.ui.activities.MainActivity
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.Util.coroutineToast
import java.io.FileNotFoundException
import java.io.IOException

/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
class CreatePlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val songs: List<Song>? = requireArguments().getParcelableArrayList(SONGS)
        val dialog = MaterialDialog(requireActivity())
            .title(R.string.new_playlist_title)
            .positiveButton(R.string.create_action)
            .negativeButton(android.R.string.cancel)
            .input(
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PERSON_NAME or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                hintRes = R.string.playlist_name_empty,
                waitForPositiveButton = true,
                allowEmpty = false
            ) { _, charSequence ->
                if (activity == null) return@input
                val name: String = charSequence.toString().trim()
                if (name.isNotEmpty()) {
                    if (!PlaylistsUtil.doesPlaylistExist(requireActivity(), name)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try { // todo
                                (requireActivity() as MainActivity).also { activity ->
                                    activity.openDocumentPicker("$name.m3u") { uri ->
                                        GlobalScope.launch(Dispatchers.IO) {
                                            if (uri == null) {
                                                coroutineToast(activity, R.string.failed)
                                            } else {
                                                LocalBroadcastManager.getInstance(App.instance).sendBroadcast(Intent(BROADCAST_PLAYLISTS_CHANGED))
                                                try {
                                                    val outputStream = activity.contentResolver.openOutputStream(uri, "rw")
                                                    if (outputStream != null) {
                                                        try {
                                                            if (songs != null) M3UWriter.write(outputStream, songs)
                                                            coroutineToast(activity, R.string.success)
                                                        } catch (e: IOException) {
                                                            coroutineToast(activity, getString(R.string.failed) + ":${uri.path} can not be written")
                                                        } finally {
                                                            outputStream.close()
                                                        }
                                                    }
                                                } catch (e: FileNotFoundException) {
                                                    coroutineToast(activity, getString(R.string.failed) + ":${uri.path} is not available")
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.i("CreatePlaylistDialog", "SaveFail: \n${e.message}")
                            }
                        } else {
                            val playlistId = PlaylistsUtil.createPlaylist(requireActivity(), name)
                            if (activity != null) {
                                if (songs != null && songs.isNotEmpty()) {
                                    dismiss()
                                    PlaylistsUtil.addToPlaylist(requireActivity(), songs, playlistId, true)
                                }
                            }
                        }
                    } else {
                        Toast.makeText(activity, requireActivity().resources.getString(R.string.playlist_exists, name), Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
            }
        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))

        return dialog
    }

    companion object {
        private const val SONGS = "songs"

        @JvmStatic
        fun create(songs: List<Song>): CreatePlaylistDialog {
            val dialog = CreatePlaylistDialog()
            val args = Bundle()
            args.putParcelableArrayList(SONGS, ArrayList(songs))
            dialog.arguments = args
            return dialog
        }
        @JvmStatic
        fun createEmpty(): CreatePlaylistDialog {
            val dialog = CreatePlaylistDialog()
            val args = Bundle()
            args.putParcelableArrayList(SONGS, null)
            dialog.arguments = args
            return dialog
        }
    }
}
