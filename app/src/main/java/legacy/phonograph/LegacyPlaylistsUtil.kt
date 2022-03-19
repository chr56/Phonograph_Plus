/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package legacy.phonograph

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import player.phonograph.App
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED
import player.phonograph.R
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.PlaylistSong
import player.phonograph.model.Song
import player.phonograph.util.PlaylistsUtil
import java.util.*
import kotlin.collections.ArrayList

object LegacyPlaylistsUtil {

    @Deprecated("use SAF")
    fun createPlaylist(context: Context, name: String): Long {
        var id: Long = -1
        if (name.isNotEmpty()) {
            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Playlists._ID/* 0 */),
                    MediaStore.Audio.PlaylistsColumns.NAME + "=?", arrayOf(name), null
                )
                if (cursor == null || cursor.count < 1) {
                    val values = ContentValues(1)
                    values.put(MediaStore.Audio.PlaylistsColumns.NAME, name)
                    val uri = context.contentResolver.insert(
                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values
                    )
                    if (uri != null) {
                        // Necessary because somehow the MediaStoreObserver doesn't work for playlists
                        context.contentResolver.notifyChange(uri, null)
                        Toast.makeText(context, context.resources.getString(R.string.created_playlist_x, name), Toast.LENGTH_SHORT).show()
                        id = uri.lastPathSegment!!.toLong()
                    }
                } else {
                    // Playlist exists
                    if (cursor.moveToFirst()) { id = cursor.getLong(0) }
                }
                cursor?.close()
            } catch (ignored: SecurityException) { }
        }
        if (id == -1L) {
            Toast.makeText(context, context.resources.getString(R.string.could_not_create_playlist), Toast.LENGTH_SHORT).show()
        }
        return id
    }

    @Deprecated("")
    fun renamePlaylist(context: Context, id: Long, newName: String) {
        val playlistUri = PlaylistsUtil.getPlaylistUris(id)
        try {
            context.contentResolver.update(playlistUri, ContentValues().also { it.put(MediaStore.Audio.PlaylistsColumns.NAME, newName) }, null, null)
            // Necessary because somehow the MediaStoreObserver doesn't work for playlists
            context.contentResolver.notifyChange(playlistUri, null)
        } catch (ignored: SecurityException) { }
    }

    /**
     * delete playlist by path via MediaStore
     * @return playlists failing to delete
     */
    @Deprecated("use SAF")
    fun deletePlaylists(context: Context, filePlaylists: List<FilePlaylist>): List<FilePlaylist> {
        var result: Int = 0
        val failList: MutableList<FilePlaylist> = ArrayList()
        // try to delete
        for (index in filePlaylists.indices) {
            val output = context.contentResolver.delete(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(filePlaylists[index].id.toString())
            )
            if (output == 0) {
                Log.w("LegacyPlaylistUtil", "fail to delete playlist ${filePlaylists[index].name}(id:${filePlaylists[index].id})")
                failList.add(filePlaylists[index])
            }
            result += output
        }
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(
                context, String.format(Locale.getDefault(), context.getString(R.string.deleted_x_playlists), result), Toast.LENGTH_SHORT
            ).show()
        }

        LocalBroadcastManager.getInstance(App.instance).sendBroadcast(Intent(BROADCAST_PLAYLISTS_CHANGED))
        return failList
    }

    @Deprecated(
        "use SAF",
        ReplaceWith(
            "addToPlaylist(context, song, playlistId, showToastOnFinish)",
            "legacy.phonograph.LegacyPlaylistsUtil.addToPlaylist"
        )
    )
    fun addToPlaylist(context: Context, song: Song, playlistId: Long, showToastOnFinish: Boolean) =
        addToPlaylist(context, listOf(song), playlistId, showToastOnFinish)

    @Deprecated(
        "use SAF",
        ReplaceWith(
            "addToPlaylist(context, listOf(song), playlistId, showToastOnFinish)",
            "legacy.phonograph.LegacyPlaylistsUtil.addToPlaylist"
        )
    )
    fun addToPlaylist(context: Context, songs: List<Song>, playlistId: Long, showToastOnFinish: Boolean) {

        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId)
        var cursor: Cursor? = null
        var base = 0
        try {
            try {
                val projection = arrayOf("max(" + MediaStore.Audio.Playlists.Members.PLAY_ORDER + ")")
                cursor = context.contentResolver
                    .query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    base = cursor.getInt(0) + 1
                }
            } finally {
                cursor?.close()
            }

            var numInserted = 0
            var offSet = 0
            while (offSet < songs.size) {
                numInserted += context.contentResolver.bulkInsert(
                    uri, makeInsertItems(songs, offSet, 1000, base)
                )
                offSet += 1000
            }

            // Necessary because somehow the MediaStoreObserver doesn't work for playlists
            context.contentResolver.notifyChange(uri, null)
            if (showToastOnFinish) {
                Toast.makeText(
                    context,
                    context.resources.getString(
                        R.string.inserted_x_songs_into_playlist_x, numInserted,
                        PlaylistsUtil.getNameForPlaylist(context, playlistId)
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (ignored: SecurityException) { }
    }

    @Deprecated("")
    private fun makeInsertItems(songs: List<Song>, offset: Int, lenth: Int, base: Int): Array<ContentValues?> {
        var len = lenth
        if (offset + len > songs.size) {
            len = songs.size - offset
        }
        val contentValues = arrayOfNulls<ContentValues>(len)
        for (i in 0 until len) {
            contentValues[i] = ContentValues()
            contentValues[i]!!.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base + offset + i)
            contentValues[i]!!.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songs[offset + i].id)
        }
        return contentValues
    }

    @Deprecated("")
    fun moveItem(context: Context, playlistId: Long, from: Int, to: Int): Boolean {
        val res = MediaStore.Audio.Playlists.Members.moveItem(context.contentResolver, playlistId, from, to)
        // Necessary because somehow the MediaStoreObserver doesn't work for playlists
        // NOTE: actually for now lets disable this because it messes with the animation (tested on Android 11)
//        context.contentResolver.notifyChange(getPlaylistUris(context, playlistId), null)
        return res
    }

    @Deprecated("")
    fun removeFromPlaylist(context: Context, song: Song, playlistId: Long) {
        val selection = MediaStore.Audio.Playlists.Members.AUDIO_ID + " =?"
        val selectionArgs = arrayOf(song.id.toString())
        try {
            if (Build.VERSION.SDK_INT >= 29)
                context.contentResolver.delete(
                    MediaStore.Audio.Playlists.Members.getContentUri(
                        MediaStore.getExternalVolumeNames(context).firstOrNull(), playlistId
                    ),
                    selection, selectionArgs
                )
            else
                context.contentResolver.delete(PlaylistsUtil.getPlaylistUris(playlistId), selection, selectionArgs)
            // Necessary because somehow the MediaStoreObserver doesn't work for playlists
            context.contentResolver.notifyChange(PlaylistsUtil.getPlaylistUris(playlistId), null)
        } catch (ignored: SecurityException) {
        }
    }

    @Deprecated("")
    fun removeFromPlaylist(context: Context, songs: List<PlaylistSong>) {
        val selectionArgs = arrayOfNulls<String>(songs.size)
        for (i in selectionArgs.indices) {
            selectionArgs[i] = songs[i].idInPlayList.toString()
        }

        var selection = MediaStore.Audio.Playlists.Members._ID + " IN ("
        for (selectionArg in selectionArgs) selection += "?, "
        selection = selection.substring(0, selection.length - 2) + ")"

        try {
            if (Build.VERSION.SDK_INT >= 29)
                context.contentResolver.delete(
                    MediaStore.Audio.Playlists.Members.getContentUri(
                        MediaStore.getExternalVolumeNames(context).firstOrNull(), songs[0].playlistId
                    ),
                    selection, selectionArgs
                )
            else
                context.contentResolver.delete(PlaylistsUtil.getPlaylistUris(songs[0].playlistId), selection, selectionArgs)
            // Necessary because somehow the MediaStoreObserver is not notified when adding a playlist
            context.contentResolver.notifyChange(PlaylistsUtil.getPlaylistUris(songs[0].playlistId), null)
        } catch (ignored: SecurityException) {
        }
    }
}
