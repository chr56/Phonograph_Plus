/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.mediastore.loaders

import legacy.phonograph.MediaStoreCompat
import player.phonograph.R
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.loader.PinedPlaylists
import player.phonograph.repo.mediastore.internal.SQLWhereClause
import player.phonograph.repo.mediastore.internal.withBasePlaylistFilter
import player.phonograph.repo.mediastore.internal.withPathFilter
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.util.mediastoreUriPlaylists
import android.content.Context
import android.database.Cursor
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.provider.BaseColumns
import android.provider.MediaStore

object PlaylistLoader : Loader<Playlist> {

    override suspend fun all(context: Context): List<Playlist> =
        queryPlaylists(context, null, null).intoPlaylists(context).sortAll(context)

    override suspend fun id(context: Context, id: Long): Playlist? =
        queryPlaylists(context, BaseColumns._ID + "=?", arrayOf(id.toString())).intoFirstPlaylist(context)

    suspend fun playlistName(context: Context, playlistName: String): Playlist? =
        queryPlaylists(
            context, MediaStoreCompat.Audio.PlaylistsColumns.NAME + "=?", arrayOf(playlistName)
        ).intoFirstPlaylist(context)

    suspend fun searchByPath(context: Context, path: String): Playlist? =
        queryPlaylists(
            context, "${MediaStoreCompat.Audio.PlaylistsColumns.DATA} = ?", arrayOf(path)
        ).intoFirstPlaylist(context)

    suspend fun searchByName(context: Context, name: String): List<Playlist> =
        queryPlaylists(
            context, "${MediaStoreCompat.Audio.PlaylistsColumns.NAME} LIKE ?", arrayOf("%$name%")
        ).intoPlaylists(context)


    /**
     * consume cursor (read & close) and convert into FilePlaylist list
     */
    private suspend fun Cursor?.intoPlaylists(context: Context): List<Playlist> =
        this?.use {
            val filePlaylists = mutableListOf<Playlist>()
            if (moveToFirst()) {
                do {
                    filePlaylists.add(
                        extractPlaylist(this, context)
                    )
                } while (moveToNext())
            }
            filePlaylists
        } ?: emptyList()

    /**
     * consume cursor (read & close) and convert into first FilePlaylist
     */
    private suspend fun Cursor?.intoFirstPlaylist(context: Context): Playlist? {
        return this?.use {
            if (moveToFirst()) extractPlaylist(this, context) else null
        }
    }

    private suspend fun extractPlaylist(cursor: Cursor, context: Context): Playlist {
        val mediastoreId = cursor.getLong(0)
        val name = cursor.getString(1)
        val path = cursor.getString(2)
        val dateAdded = cursor.getLong(3)
        val dateModified = cursor.getLong(4)
        val storageVolume = if (SDK_INT > Q) cursor.getString(5) else MEDIASTORE_VOLUME_EXTERNAL
        val iconRes = when {
            PinedPlaylists.isPined(context, mediastoreId, path)    -> R.drawable.ic_pin_white_24dp
            name == context.getString(R.string.playlist_favorites) -> R.drawable.ic_favorite_white_24dp
            else                                                   -> R.drawable.ic_file_music_white_24dp
        }
        return Playlist(
            name = name,
            location = FilePlaylistLocation(
                path = path,
                storageVolume = storageVolume,
                mediastoreId = mediastoreId
            ),
            dateAdded = dateAdded,
            dateModified = dateModified,
            iconRes = iconRes
        )
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
            withPathFilter(context, escape = false) {
                SQLWhereClause(
                    selection = withBasePlaylistFilter { selection },
                    selectionValues = selectionValues ?: emptyArray()
                )
            }
        return context.contentResolver.query(
            mediastoreUriPlaylists(MEDIASTORE_VOLUME_EXTERNAL),
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
            .query(mediastoreUriPlaylists(MEDIASTORE_VOLUME_EXTERNAL), arrayOf(), selection, values, null)
            ?.use { it.count > 0 } ?: false

    private fun List<Playlist>.sortAll(context: Context): List<Playlist> {
        val sortMode = Setting(context)[Keys.playlistSortMode].data
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