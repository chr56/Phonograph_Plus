/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.mediastore.loaders

import legacy.phonograph.MediaStoreCompat
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.internal.SQLWhereClause
import player.phonograph.repo.mediastore.internal.withBasePlaylistFilter
import player.phonograph.repo.mediastore.internal.withPathFilter
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.MEDIASTORE_VOLUME_EXTERNAL
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.provider.BaseColumns
import android.provider.MediaStore

object PlaylistLoader : Loader<Playlist> {

    override suspend fun all(context: Context): List<Playlist> =
        queryPlaylists(context, null, null).intoPlaylists().sortAll(context)

    override suspend fun id(context: Context, id: Long): Playlist? =
        queryPlaylists(context, BaseColumns._ID + "=?", arrayOf(id.toString())).intoFirstPlaylist()

    fun playlistName(context: Context, playlistName: String): Playlist? =
        queryPlaylists(
            context, MediaStoreCompat.Audio.PlaylistsColumns.NAME + "=?", arrayOf(playlistName)
        ).intoFirstPlaylist()

    fun searchByPath(context: Context, path: String): Playlist? =
        queryPlaylists(
            context, "${MediaStoreCompat.Audio.PlaylistsColumns.DATA} = ?", arrayOf(path)
        ).intoFirstPlaylist()

    fun searchByName(context: Context, name: String): List<Playlist> =
        queryPlaylists(
            context, "${MediaStoreCompat.Audio.PlaylistsColumns.NAME} LIKE ?", arrayOf("%$name%")
        ).intoPlaylists()


    /**
     * consume cursor (read & close) and convert into FilePlaylist list
     */
    private fun Cursor?.intoPlaylists(): List<Playlist> =
        this?.use {
            val filePlaylists = mutableListOf<Playlist>()
            if (moveToFirst()) {
                do {
                    filePlaylists.add(
                        extractPlaylist(this)
                    )
                } while (moveToNext())
            }
            filePlaylists
        } ?: emptyList()

    /**
     * consume cursor (read & close) and convert into first FilePlaylist
     */
    private fun Cursor?.intoFirstPlaylist(): Playlist? {
        return this?.use {
            if (moveToFirst()) extractPlaylist(this) else null
        }
    }

    private fun extractPlaylist(cursor: Cursor): Playlist = Playlist(
        name = cursor.getString(1),
        location = FilePlaylistLocation(
            path = cursor.getString(2),
            storageVolume = if (SDK_INT > Q) cursor.getString(5) else MEDIASTORE_VOLUME_EXTERNAL,
            mediastoreId = cursor.getLong(0)
        ),
        dateAdded = cursor.getLong(3),
        dateModified = cursor.getLong(4),
    )

    /**
     * query playlist file via MediaStore
     */
    private fun queryPlaylists(
        context: Context,
        selection: String?,
        selectionValues: Array<String>?,
    ): Cursor? {
        val actual =
            withPathFilter(context, escape = false) {
                SQLWhereClause(
                    selection = withBasePlaylistFilter { selection },
                    selectionValues = selectionValues ?: emptyArray()
                )
            }
        return context.contentResolver.query(
            MediaStoreCompat.Audio.Playlists.EXTERNAL_CONTENT_URI,
            if (SDK_INT > Q) BASE_PLAYLIST_PROJECTION_Q else BASE_PLAYLIST_PROJECTION,
            actual.selection,
            actual.selectionValues,
            MediaStoreCompat.Audio.Playlists.DEFAULT_SORT_ORDER
        )
    }

    private val BASE_PLAYLIST_PROJECTION_Q
        get() = arrayOf(
            BaseColumns._ID, // 0
            MediaStoreCompat.Audio.PlaylistsColumns.NAME, // 1
            MediaStoreCompat.Audio.PlaylistsColumns.DATA, // 2
            MediaStoreCompat.Audio.PlaylistsColumns.DATE_ADDED, // 3
            MediaStoreCompat.Audio.PlaylistsColumns.DATE_MODIFIED, // 4
            MediaStore.MediaColumns.VOLUME_NAME, // 5
        )

    private val BASE_PLAYLIST_PROJECTION
        get() = arrayOf(
            BaseColumns._ID, // 0
            MediaStoreCompat.Audio.PlaylistsColumns.NAME, // 1
            MediaStoreCompat.Audio.PlaylistsColumns.DATA, // 2
            MediaStoreCompat.Audio.PlaylistsColumns.DATE_ADDED, // 3
            MediaStoreCompat.Audio.PlaylistsColumns.DATE_MODIFIED, // 4
        )

    fun checkExistence(context: Context, name: String): Boolean =
        checkExistenceImpl(
            context = context,
            selection = MediaStoreCompat.Audio.PlaylistsColumns.NAME + "=?",
            values = arrayOf(name)
        )

    fun checkExistence(context: Context, playlistId: Long): Boolean =
        playlistId != -1L && checkExistenceImpl(
            context = context,
            selection = MediaStoreCompat.Audio.Playlists._ID + "=?",
            values = arrayOf(playlistId.toString())
        )

    private fun checkExistenceImpl(context: Context, selection: String, values: Array<String>): Boolean =
        context.contentResolver
            .query(MediaStoreCompat.Audio.Playlists.EXTERNAL_CONTENT_URI, arrayOf(), selection, values, null)
            ?.use { it.count > 0 } ?: false

    /**
     * @param id playlist id
     * @return playlist members uri in MediaStore
     */
    fun mediastoreMembersUri(volume: String, id: Long): Uri =
        MediaStoreCompat.Audio.Playlists.Members.getContentUri(volume, id)

    /**
     * @param volume MediaStore volume name
     * @param id playlist id
     * @return playlist uri in MediaStore
     */
    fun mediastoreUri(volume: String, id: Long): Uri =
        ContentUris.withAppendedId(MediaStoreCompat.Audio.Playlists.getContentUri(volume), id)

    private fun List<Playlist>.sortAll(context: Context): List<Playlist> {
        val sortMode = Setting(context).Composites[Keys.playlistSortMode].data
        val revert = sortMode.revert
        return when (sortMode.sortRef) {
            SortRef.DISPLAY_NAME  -> this.sort(revert) { it.name }
            SortRef.PATH          -> this.sort(revert) { it.location }
            SortRef.ADDED_DATE    -> this.sort(revert) { it.dateAdded }
            SortRef.MODIFIED_DATE -> this.sort(revert) { it.dateModified }
            else                  -> this
        }
    }

    private inline fun List<Playlist>.sort(
        revert: Boolean,
        crossinline selector: (Playlist) -> Comparable<*>?,
    ): List<Playlist> {
        return if (revert) this.sortedWith(compareByDescending(selector))
        else this.sortedWith(compareBy(selector))
    }
}