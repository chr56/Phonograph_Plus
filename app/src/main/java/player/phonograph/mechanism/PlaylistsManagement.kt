/*
 *  Copyright (c) 2022~2023 chr_56
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package player.phonograph.mechanism

import legacy.phonograph.MediaStoreCompat.Audio.Playlists
import legacy.phonograph.MediaStoreCompat.Audio.PlaylistsColumns
import player.phonograph.model.playlist.FilePlaylist
import androidx.documentfile.provider.DocumentFile
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import android.util.Log

object PlaylistsManagement {
    private const val TAG: String = "PlaylistsManagement"

    fun getPlaylistPath(context: Context, filePlaylist: FilePlaylist): String {
        val cursor = context.contentResolver.query(
            Playlists.EXTERNAL_CONTENT_URI,
            arrayOf(
                BaseColumns._ID /* 0 */,
                PlaylistsColumns.NAME /* 1 */,
                PlaylistsColumns.DATA /* 2 */
            ),
            "${BaseColumns._ID} = ? AND ${PlaylistsColumns.NAME} = ?",
            arrayOf(filePlaylist.id.toString(), filePlaylist.name), null
        )
        var path = "-"
        cursor?.use {
            it.moveToFirst()
            path = it.getString(2)
        }
        return path
    }

    fun doesPlaylistExist(context: Context, name: String): Boolean {
        return doesPlaylistExistImp(context, PlaylistsColumns.NAME + "=?", arrayOf(name))
    }

    fun doesPlaylistExist(context: Context, playlistId: Long): Boolean =
        playlistId != -1L && doesPlaylistExistImp(context, Playlists._ID + "=?", arrayOf(playlistId.toString()))

    private fun doesPlaylistExistImp(context: Context, selection: String, values: Array<String>): Boolean =
        context.contentResolver
            .query(Playlists.EXTERNAL_CONTENT_URI, arrayOf(), selection, values, null)
            ?.use { it.count >= 0 } ?: false

    fun getNameForPlaylist(context: Context, id: Long): String {
        val cursor = context.contentResolver.query(
            ContentUris.withAppendedId(Playlists.EXTERNAL_CONTENT_URI, id),
            arrayOf(PlaylistsColumns.NAME), null, null, null
        ) ?: return ""
        return if (cursor.moveToFirst()) {
            cursor.use { it.getString(0).orEmpty() }
        } else {
            ""
        }
    }

    /**
     * WARNING: random order (perhaps)
     */
    fun getPlaylistFileNames(context: Context, filePlaylists: List<FilePlaylist>): List<String> {

        val ids: List<Long> = List(filePlaylists.size) { filePlaylists[it].id }

        val cursor = context.contentResolver.query(
            Playlists.EXTERNAL_CONTENT_URI,
            arrayOf(
                BaseColumns._ID /* 0 */,
                MediaStore.MediaColumns.DISPLAY_NAME /* 1 */
            ),
            null, null, null
        )

        val displayNames = mutableListOf<String>()
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    ids.forEach { id ->
                        if (cursor.getLong(0) == id) {
                            displayNames.add(cursor.getString(1))
                        }
                    }
                } while (cursor.moveToNext())
            }
        }

        return displayNames.ifEmpty { emptyList() }
    }

    fun getPlaylistUris(filePlaylist: FilePlaylist): Uri = getPlaylistUris(filePlaylist.id)

    fun getPlaylistUris(playlistId: Long): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            Playlists.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, playlistId)
        else {
            Playlists.Members.getContentUri("external", playlistId)
        }

    fun doesPlaylistContain(context: Context, playlistId: Long, songId: Long): Boolean {
        if (playlistId != -1L) {
            val cursor = context.contentResolver.query(
                getPlaylistUris(playlistId),
                arrayOf(Playlists.Members.AUDIO_ID),
                Playlists.Members.AUDIO_ID + "=?", arrayOf(songId.toString()), null
            ) ?: return false
            return cursor.use { it.count > 0 }
        }
        return false
    }


    fun searchPlaylist(context: Context, dir: DocumentFile, filePlaylists: List<FilePlaylist>): List<DocumentFile> {
        val fileNames = getPlaylistFileNames(context, filePlaylists)
        if (fileNames.isEmpty()) {
            Log.w(TAG, "No playlist display name?")
            return mutableListOf()
        }
        return searchFiles(context, dir, fileNames)
    }

    fun searchFiles(context: Context, dir: DocumentFile, filenames: List<String>): List<DocumentFile> {
        if (dir.isFile or dir.isVirtual) return mutableListOf()
        val result = mutableListOf<DocumentFile>()
        if (dir.isDirectory) {
            dir.listFiles().forEach { sub ->
                if (sub.isFile) {
                    filenames.forEach { n ->
                        if (n == sub.name.orEmpty()) result.add(sub)
                    }
                }
                if (sub.isDirectory) {
                    result.addAll(
                        searchFiles(context, sub, filenames)
                    )
                }
            }
        }
        return result
    }

}
