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

//                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
//                    addCategory(Intent.CATEGORY_OPENABLE)
//                    type = "audio/x-mpegurl"
//                    putExtra(
//                        Intent.EXTRA_TITLE,
//                        playlist.name +
//                            SimpleDateFormat(
//                                "_yy-MM-dd_HH-mm", Locale.getDefault()
//                            ).format(Calendar.getInstance().time)
//                    )
//                }
//
//                startActivityForResult(activity, intent, REQUEST_CODE_SAVE_PLAYLIST, null)
//
//                val msgBundle = Bundle()
//                if (playlist is AbsSmartPlaylist) {
//                    val type: String = when (playlist) {
//                        is HistoryPlaylist -> HistoryPlaylist
//                        is LastAddedPlaylist -> LastAddedPlaylist
//                        is MyTopTracksPlaylist -> MyTopTracksPlaylist
//                        else -> "Smart"
//                    }
//                    msgBundle.putString(TYPE, type)
//                } else {
//                    msgBundle.putString(TYPE, NormalPlaylist)
//                    msgBundle.putLong(PLAYLIST_ID, playlist.id)
//                }
//
//                (activity as AbsMusicServiceActivity).handler?.let { handler ->
//                    val msg = Message.obtain(handler, REQUEST_CODE_SAVE_PLAYLIST).also {
//                        it.data = msgBundle
//                    }
//                    handler.sendMessageDelayed(msg, 10)
//                }
            }
        }
        return false
    }

    private fun getPlaylistSongs(activity: Activity, playlist: Playlist): List<Song> {
        return if (playlist is AbsCustomPlaylist) playlist.getSongs(activity)
        else PlaylistSongLoader.getPlaylistSongList(activity, playlist.id)
    }
}
