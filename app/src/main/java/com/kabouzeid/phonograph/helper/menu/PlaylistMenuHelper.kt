package com.kabouzeid.phonograph.helper.menu

import android.app.Activity
import android.content.Context
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kabouzeid.phonograph.App
import com.kabouzeid.phonograph.R
import com.kabouzeid.phonograph.dialogs.AddToPlaylistDialog
import com.kabouzeid.phonograph.dialogs.DeletePlaylistDialog
import com.kabouzeid.phonograph.dialogs.RenamePlaylistDialog
import com.kabouzeid.phonograph.helper.MusicPlayerRemote
import com.kabouzeid.phonograph.loader.PlaylistSongLoader
import com.kabouzeid.phonograph.misc.WeakContextAsyncTask
import com.kabouzeid.phonograph.model.AbsCustomPlaylist
import com.kabouzeid.phonograph.model.Playlist
import com.kabouzeid.phonograph.model.Song
import com.kabouzeid.phonograph.util.PlaylistsUtil
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
}
