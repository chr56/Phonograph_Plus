package player.phonograph.helper.menu

import android.app.Activity
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import player.phonograph.*
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.DeletePlaylistDialog
import player.phonograph.dialogs.RenamePlaylistDialog
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.model.AbsCustomPlaylist
import player.phonograph.model.BasePlaylist
import player.phonograph.model.Song
import player.phonograph.util.SAFCallbackHandlerActivity
import util.phonograph.m3u.PlaylistsManager
import kotlin.collections.ArrayList

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object PlaylistMenuHelper {

    @JvmStatic
    fun handleMenuClick(activity: AppCompatActivity, basePlaylist: BasePlaylist, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_play -> {
                MusicPlayerRemote.openQueue(
                    ArrayList(getPlaylistSongs(activity, basePlaylist)), 0, true
                )
                return true
            }
            R.id.action_play_next -> {
                MusicPlayerRemote.playNext(ArrayList(getPlaylistSongs(activity, basePlaylist)))
                return true
            }
            R.id.action_add_to_current_playing -> {
                MusicPlayerRemote.enqueue(ArrayList(getPlaylistSongs(activity, basePlaylist)))
                return true
            }
            R.id.action_add_to_playlist -> {
                AddToPlaylistDialog.create(getPlaylistSongs(activity, basePlaylist))
                    .show(activity.supportFragmentManager, "ADD_PLAYLIST")
                return true
            }
            R.id.action_rename_playlist -> {
                RenamePlaylistDialog.create(basePlaylist.id).show(activity.supportFragmentManager, "RENAME_PLAYLIST")
                return true
            }
            R.id.action_delete_playlist -> {
                DeletePlaylistDialog.create(listOf(basePlaylist)).show(
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

    private fun getPlaylistSongs(activity: Activity, basePlaylist: BasePlaylist): List<Song> {
        return if (basePlaylist is AbsCustomPlaylist) basePlaylist.getSongs(activity)
        else PlaylistSongLoader.getPlaylistSongList(activity, basePlaylist.id)
    }
}
