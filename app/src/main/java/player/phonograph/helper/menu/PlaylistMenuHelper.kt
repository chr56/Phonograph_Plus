package player.phonograph.helper.menu

import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import player.phonograph.R
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.DeletePlaylistDialog
import player.phonograph.dialogs.RenamePlaylistDialog
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.model.BasePlaylist
import player.phonograph.model.FilePlaylist
import player.phonograph.util.SAFCallbackHandlerActivity
import util.phonograph.m3u.PlaylistsManager

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object PlaylistMenuHelper {

    @JvmStatic
    fun handleMenuClick(activity: AppCompatActivity, basePlaylist: BasePlaylist, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_play -> {
                MusicPlayerRemote.openQueue(
                    ArrayList(basePlaylist.getSongs(activity)), 0, true
                )
                return true
            }
            R.id.action_play_next -> {
                MusicPlayerRemote.playNext(ArrayList(basePlaylist.getSongs(activity)))
                return true
            }
            R.id.action_add_to_current_playing -> {
                MusicPlayerRemote.enqueue(ArrayList(basePlaylist.getSongs(activity)))
                return true
            }
            R.id.action_add_to_playlist -> {
                AddToPlaylistDialog.create(basePlaylist.getSongs(activity))
                    .show(activity.supportFragmentManager, "ADD_PLAYLIST")
                return true
            }
            R.id.action_rename_playlist -> {
                RenamePlaylistDialog.create(basePlaylist.id).show(activity.supportFragmentManager, "RENAME_PLAYLIST")
                return true
            }
            R.id.action_delete_playlist -> {
                DeletePlaylistDialog.create(listOf(basePlaylist as FilePlaylist)).show(
                    activity.supportFragmentManager, "DELETE_PLAYLIST"
                )
                return true
            }
            R.id.action_save_playlist -> {
                GlobalScope.launch(Dispatchers.Default) {

                    if (activity is SAFCallbackHandlerActivity) {
                        PlaylistsManager(activity, activity).duplicatePlaylistViaSaf(basePlaylist)
                    } else {
                        PlaylistsManager(activity, null).duplicatePlaylistViaSaf(basePlaylist)
                    }
                }
                return true
            }
        }
        return false
    }
}
