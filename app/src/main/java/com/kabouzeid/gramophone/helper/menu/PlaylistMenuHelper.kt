package com.kabouzeid.gramophone.helper.menu

import android.app.Activity
import android.content.Context
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kabouzeid.gramophone.App
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.dialogs.AddToPlaylistDialog
import com.kabouzeid.gramophone.dialogs.DeletePlaylistDialog
import com.kabouzeid.gramophone.dialogs.RenamePlaylistDialog
import com.kabouzeid.gramophone.helper.MusicPlayerRemote
import com.kabouzeid.gramophone.loader.PlaylistSongLoader
import com.kabouzeid.gramophone.misc.WeakContextAsyncTask
import com.kabouzeid.gramophone.model.AbsCustomPlaylist
import com.kabouzeid.gramophone.model.Playlist
import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.util.PlaylistsUtil
import java.io.IOException

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
                SavePlaylistAsyncTask(activity).execute(playlist)
                return true
            }
        }
        return false
    }

    private fun getPlaylistSongs(activity: Activity, playlist: Playlist): List<Song?> {
        return if (playlist is AbsCustomPlaylist) playlist.getSongs(activity)
        else PlaylistSongLoader.getPlaylistSongList(activity, playlist.id)
    }

    private class SavePlaylistAsyncTask(context: Context?) :
        WeakContextAsyncTask<Playlist?, String?, String?>(context) {
        override fun doInBackground(vararg params: Playlist?): String? {
            return try {
                String.format(
                    App.getInstance().applicationContext.getString(R.string.saved_playlist_to),
                    PlaylistsUtil.savePlaylist(App.getInstance().applicationContext, params[0])
                )
            } catch (e: IOException) {
                e.printStackTrace()
                String.format(
                    App.getInstance().applicationContext.getString(R.string.failed_to_save_playlist), e
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
}
