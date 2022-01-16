package player.phonograph.helper.menu

import android.app.Activity
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
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
import player.phonograph.model.Playlist
import player.phonograph.model.Song
import player.phonograph.util.PlaylistWriter
import player.phonograph.util.SAFCallbackHandlerActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object PlaylistMenuHelper {

    @JvmStatic
    fun handleMenuClick(activity: AppCompatActivity, playlist: Playlist, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_play -> {
                MusicPlayerRemote.openQueue(
                    ArrayList(getPlaylistSongs(activity, playlist)), 0, true
                )
                return true
            }
            R.id.action_play_next -> {
                MusicPlayerRemote.playNext(ArrayList(getPlaylistSongs(activity, playlist)))
                return true
            }
            R.id.action_add_to_current_playing -> {
                MusicPlayerRemote.enqueue(ArrayList(getPlaylistSongs(activity, playlist)))
                return true
            }
            R.id.action_add_to_playlist -> {
                AddToPlaylistDialog.create(getPlaylistSongs(activity, playlist))
                    .show(activity.supportFragmentManager, "ADD_PLAYLIST")
                return true
            }
            R.id.action_rename_playlist -> {
                RenamePlaylistDialog.create(playlist.id).show(activity.supportFragmentManager, "RENAME_PLAYLIST")
                return true
            }
            R.id.action_delete_playlist -> {
                DeletePlaylistDialog.create(listOf(playlist)).show(
                    activity.supportFragmentManager, "DELETE_PLAYLIST"
                )
                return true
            }
            R.id.action_save_playlist -> {

                if (activity is SAFCallbackHandlerActivity) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val filename = playlist.name + SimpleDateFormat("_yy-MM-dd_HH-mm", Locale.getDefault()).format(
                            Calendar.getInstance().time
                        )
                        val songs: List<Song> =
                            when (playlist) {
                                is AbsCustomPlaylist -> {
                                    playlist.getSongs(activity)
                                }
                                else -> {
                                    PlaylistSongLoader.getPlaylistSongList(activity, playlist.id)
                                }
                            }

                        PlaylistWriter.createPlaylistViaSAF(filename, songs, activity)
                    }
                    return true
                } else {
                    try {
                        Looper.prepare()
                        Toast.makeText(activity, R.string.failed, Toast.LENGTH_SHORT).show()
                        Looper.loop()
                    } catch (e: Exception) { Log.w("SAVE_PLAYLIST", e.message.orEmpty()) }
                }
            }
        }
        return false
    }

    private fun getPlaylistSongs(activity: Activity, playlist: Playlist): List<Song> {
        return if (playlist is AbsCustomPlaylist) playlist.getSongs(activity)
        else PlaylistSongLoader.getPlaylistSongList(activity, playlist.id)
    }
}
