/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mediastore

import legacy.phonograph.MediaStoreCompat
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.sort.SortRef
import player.phonograph.settings.Setting
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns

object PlaylistLoader {

    fun allPlaylists(context: Context): List<FilePlaylist> =
        queryPlaylists(context, null, null).intoPlaylists().sortAll()

    fun playlistId(context: Context, playlistId: Long): FilePlaylist =
        queryPlaylists(
            context, BaseColumns._ID + "=?", arrayOf(playlistId.toString())
        ).intoFirstPlaylist()

    fun playlistName(context: Context, playlistName: String): FilePlaylist =
        queryPlaylists(
            context, MediaStoreCompat.Audio.PlaylistsColumns.NAME + "=?", arrayOf(playlistName)
        ).intoFirstPlaylist()

    fun searchByPath(context: Context, path: String): FilePlaylist? =
        queryPlaylists(
            context, "${MediaStoreCompat.Audio.PlaylistsColumns.DATA} = ?", arrayOf(path)
        ).intoFirstPlaylist().takeIf { it.id > 0 }


    /**
     * consume cursor (read & close) and convert into FilePlaylist list
     */
    private fun Cursor?.intoPlaylists(): List<FilePlaylist> =
        this?.use {
            val filePlaylists = mutableListOf<FilePlaylist>()
            if (moveToFirst()) {
                do {
                    filePlaylists.add(
                        FilePlaylist(
                            id = getLong(0),
                            name = getString(1),
                            path = getString(2),
                            dateAdded = getLong(3),
                            dateModified = getLong(4),
                        )
                    )
                } while (moveToNext())
            }
            filePlaylists
        } ?: emptyList()

    /**
     * consume cursor (read & close) and convert into first FilePlaylist
     */
    fun Cursor?.intoFirstPlaylist(): FilePlaylist {
        return this?.use {
            if (moveToFirst())
                FilePlaylist(
                    id = getLong(0),
                    name = getString(1),
                    path = getString(2),
                    dateAdded = getLong(3),
                    dateModified = getLong(4),
                )
            else
                FilePlaylist.EMPTY_PLAYLIST
        } ?: FilePlaylist.EMPTY_PLAYLIST
    }

    /**
     * query playlist file via MediaStore
     */
    private fun queryPlaylists(
        context: Context,
        selection: String?,
        selectionValues: Array<String>?,
    ): Cursor? {
        val actual =
            withPathFilter(context) {
                SQLWhereClause(
                    selection = withBasePlaylistFilter { selection },
                    selectionValues = selectionValues ?: emptyArray()
                )
            }
        return context.contentResolver.query(
            MediaStoreCompat.Audio.Playlists.EXTERNAL_CONTENT_URI,
            BASE_PLAYLIST_PROJECTION,
            actual.selection,
            actual.selectionValues,
            MediaStoreCompat.Audio.Playlists.DEFAULT_SORT_ORDER
        )
    }

    private val BASE_PLAYLIST_PROJECTION = arrayOf(
        BaseColumns._ID, // 0
        MediaStoreCompat.Audio.PlaylistsColumns.NAME, // 1
        MediaStoreCompat.Audio.PlaylistsColumns.DATA, // 2
        MediaStoreCompat.Audio.PlaylistsColumns.DATE_ADDED, // 3
        MediaStoreCompat.Audio.PlaylistsColumns.DATE_MODIFIED, // 4
    )

    private fun List<FilePlaylist>.sortAll(): List<FilePlaylist> {
        val revert = Setting.instance.playlistSortMode.revert
        return when (Setting.instance.playlistSortMode.sortRef) {
            SortRef.DISPLAY_NAME -> this.sort(revert) { it.name }
            SortRef.PATH         -> this.sort(revert) { it.associatedFilePath }
            else                 -> this
        }
    }

    private inline fun List<FilePlaylist>.sort(
        revert: Boolean,
        crossinline selector: (FilePlaylist) -> Comparable<*>?,
    ): List<FilePlaylist> {
        return if (revert) this.sortedWith(compareByDescending(selector))
        else this.sortedWith(compareBy(selector))
    }
}