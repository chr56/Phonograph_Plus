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
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.util.SAFCallbackHandlerActivity
import util.phonograph.m3u.PlaylistsManager

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object PlaylistMenuHelper {

    @JvmStatic
    fun handleMenuClick(activity: AppCompatActivity, playlist: Playlist, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_play -> {
                MusicPlayerRemote.openQueue(
                    ArrayList(playlist.getSongs(activity)), 0, true
                )
                return true
            }
            R.id.action_play_next -> {
                MusicPlayerRemote.playNext(ArrayList(playlist.getSongs(activity)))
                return true
            }
            R.id.action_add_to_current_playing -> {
                MusicPlayerRemote.enqueue(ArrayList(playlist.getSongs(activity)))
                return true
            }
            R.id.action_add_to_playlist -> {
                AddToPlaylistDialog.create(playlist.getSongs(activity))
                    .show(activity.supportFragmentManager, "ADD_PLAYLIST")
                return true
            }
            R.id.action_rename_playlist -> {
                RenamePlaylistDialog.create(playlist.id).show(activity.supportFragmentManager, "RENAME_PLAYLIST")
                return true
            }
            R.id.action_delete_playlist -> {
                DeletePlaylistDialog.create(listOf(playlist as FilePlaylist)).show(
                    activity.supportFragmentManager, "DELETE_PLAYLIST"
                )
                return true
            }
            R.id.action_save_playlist -> {
                GlobalScope.launch(Dispatchers.Default) {

                    if (activity is SAFCallbackHandlerActivity) {
                        PlaylistsManager(activity, activity).duplicatePlaylistViaSaf(playlist)
                    } else {
                        PlaylistsManager(activity, null).duplicatePlaylistViaSaf(playlist)
                    }
                }
                return true
            }
        }
        return false
    }
}
