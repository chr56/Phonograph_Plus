package player.phonograph.helper.menu

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import player.phonograph.App
import player.phonograph.R
import player.phonograph.Task
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.DeletePlaylistDialog
import player.phonograph.dialogs.RenamePlaylistDialog
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.misc.WeakContextAsyncTask
import player.phonograph.model.AbsCustomPlaylist
import player.phonograph.model.Playlist
import player.phonograph.model.Song
import player.phonograph.model.smartplaylist.AbsSmartPlaylist
import player.phonograph.model.smartplaylist.HistoryPlaylist
import player.phonograph.model.smartplaylist.LastAddedPlaylist
import player.phonograph.model.smartplaylist.MyTopTracksPlaylist
import player.phonograph.util.FileSaver.savePlaylist
import player.phonograph.util.PlaylistsUtil
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.ArrayList

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object PlaylistMenuHelper {
    const val TASK_ID_SAVE_PLAYLIST: Int = 909090
    const val TASK_DATA_HistoryPlaylist = "HistoryPlaylist"
    const val TASK_DATA_LastAddedPlaylist = "LastAddedPlaylist"
    const val TASK_DATA_MyTopTracksPlaylist = "MyTopTracksPlaylist"
    const val TASK_DATA_Normal = "Normal"

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

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "audio/x-mpegurl"
                    putExtra(
                        Intent.EXTRA_TITLE,
                        playlist.name +
                            SimpleDateFormat(
                                "_yy-MM-dd_HH-mm", Locale.getDefault()
                            ).format(Calendar.getInstance().time)
                    )
                }
                App.instance.taskManager.addTask(
                    Task(TASK_ID_SAVE_PLAYLIST, null).also {
                        it.action = Task.ACTION_SAVE_PLAYLIST

                        if (playlist is AbsSmartPlaylist) {
                            it.data =
                                when (playlist) {
                                    is HistoryPlaylist -> TASK_DATA_HistoryPlaylist
                                    is LastAddedPlaylist -> TASK_DATA_LastAddedPlaylist
                                    is MyTopTracksPlaylist -> TASK_DATA_MyTopTracksPlaylist
                                    else -> "Smart"
                                }
                        } else {
                            it.data = TASK_DATA_Normal
                            it.num = playlist.id
                        }
                    }
                )
                startActivityForResult(activity, intent, TASK_ID_SAVE_PLAYLIST, null)

                return true
            }
        }
        return false
    }

    private fun getPlaylistSongs(activity: Activity, playlist: Playlist): List<Song> {
        return if (playlist is AbsCustomPlaylist) playlist.getSongs(activity)
        else PlaylistSongLoader.getPlaylistSongList(activity, playlist.id)
    }

    private class SavePlaylistAsyncTask(context: Context?) :
        WeakContextAsyncTask<Playlist?, String?, String?>(context) {
        override fun doInBackground(vararg params: Playlist?): String? {
            return try {
                String.format(
                    App.instance.applicationContext.getString(R.string.saved_playlist_to),
                    PlaylistsUtil.savePlaylist(App.instance.applicationContext, params[0])
                )
            } catch (e: IOException) {
                e.printStackTrace()
                String.format(
                    App.instance.applicationContext.getString(R.string.failed_to_save_playlist), e
                )
            }
        }

        override fun onPostExecute(string: String?) {
            super.onPostExecute(string)
            val context = context
            if (context != null) {
                Toast.makeText(context, string, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * CALL this IN onActivityResult()
     */
    @JvmStatic
    fun handleSavePlaylist(activity: Activity, uri: Uri) {

        val taskManager = App.instance.taskManager

        val task = taskManager.findTask(TASK_ID_SAVE_PLAYLIST) ?: return
        val action: String = task.action ?: return

        if (action == Task.ACTION_SAVE_PLAYLIST) {
            val data: String = task.data ?: return
            var result: Short = -1
            try {
                when (data) {
                    TASK_DATA_Normal ->
                        task.num?.let { result = savePlaylist(activity, uri, it) }
                    TASK_DATA_MyTopTracksPlaylist ->
                        result = savePlaylist(activity, uri, MyTopTracksPlaylist(activity))
                    TASK_DATA_LastAddedPlaylist ->
                        result = savePlaylist(activity, uri, LastAddedPlaylist(activity))
                    TASK_DATA_HistoryPlaylist ->
                        result = savePlaylist(activity, uri, HistoryPlaylist(activity))
                    else -> {
                        result = -1
                    }
                }

                // report result
                if (result.toInt() != 0) {
                    Toast.makeText(
                        activity, activity.resources.getText(R.string.failed_to_save_playlist, "_"), Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        activity, activity.resources.getText(R.string.success), Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                taskManager.removeTask(TASK_ID_SAVE_PLAYLIST)
            }
        }
    }
}
