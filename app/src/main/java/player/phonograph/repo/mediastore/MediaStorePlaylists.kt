/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.R
import player.phonograph.foundation.compat.MediaStoreCompat.Audio.Playlists
import player.phonograph.foundation.compat.MediaStoreCompat.Audio.PlaylistsColumns
import player.phonograph.foundation.error.record
import player.phonograph.foundation.mediastore.PlaylistLookup
import player.phonograph.foundation.mediastore.intoFirstPlaylist
import player.phonograph.foundation.mediastore.intoPlaylistSongs
import player.phonograph.foundation.mediastore.intoPlaylists
import player.phonograph.foundation.mediastore.mediastorePlaylistSortRefKey
import player.phonograph.foundation.mediastore.mediastoreUriPlaylistMembers
import player.phonograph.foundation.mediastore.mediastoreUriPlaylistsExternal
import player.phonograph.foundation.mediastore.queryMediastorePlaylistSongs
import player.phonograph.foundation.mediastore.queryMediastorePlaylists
import player.phonograph.foundation.mediastore.withBasePlaylistFilter
import player.phonograph.model.PlaylistSong
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.PlaylistLocation
import player.phonograph.model.repo.loader.IPlaylists
import player.phonograph.model.sort.SortMode
import player.phonograph.repo.loader.PinedPlaylists
import player.phonograph.repo.mediastore.internal.withPathFilter
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.sort
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns

object MediaStorePlaylists : IPlaylists {

    override suspend fun all(context: Context): List<Playlist> =
        queryPlaylists(
            context, null, null
        ).intoPlaylists(IconLookup(context)).sortAll(context)

    suspend fun id(context: Context, id: Long): Playlist? =
        queryPlaylists(
            context, BaseColumns._ID + "=?", arrayOf(id.toString())
        ).intoFirstPlaylist(IconLookup(context))

    override suspend fun of(context: Context, location: PlaylistLocation): Playlist? =
        id(context, (location as FilePlaylistLocation).mediastoreId)

    fun songs(context: Context, playlistId: Long): List<PlaylistSong> =
        try {
            queryMediastorePlaylistSongs(context, playlistId).intoPlaylistSongs(playlistId)
        } catch (_: SecurityException) {
            emptyList()
        }

    override suspend fun songs(context: Context, location: PlaylistLocation): List<PlaylistSong> =
        songs(context, (location as FilePlaylistLocation).mediastoreId)

    override suspend fun contains(context: Context, location: PlaylistLocation, songId: Long): Boolean =
        if (location is FilePlaylistLocation) {
            contains(context, location.storageVolume, location.mediastoreId, songId)
        } else {
            false
        }

    override suspend fun named(context: Context, name: String): Playlist? =
        queryPlaylists(
            context,
            PlaylistsColumns.NAME + "=?", arrayOf(name)
        ).intoFirstPlaylist(IconLookup(context))

    override suspend fun exists(context: Context, location: PlaylistLocation): Boolean =
        checkExistence(context, (location as FilePlaylistLocation).mediastoreId)

    override suspend fun searchByName(context: Context, query: String): List<Playlist> =
        queryPlaylists(
            context, "${PlaylistsColumns.NAME} LIKE ?", arrayOf("%$query%")
        ).intoPlaylists(IconLookup(context))

    suspend fun searchByPath(context: Context, path: String): Playlist? =
        queryPlaylists(
            context, "${PlaylistsColumns.DATA} = ?", arrayOf(path)
        ).intoFirstPlaylist(IconLookup(context))



    fun checkExistence(context: Context, name: String): Boolean =
        checkExistenceImpl(
            context = context,
            selection = PlaylistsColumns.NAME + "=?",
            values = arrayOf(name)
        )

    fun checkExistence(context: Context, playlistId: Long): Boolean =
        playlistId != -1L && checkExistenceImpl(
            context = context,
            selection = Playlists._ID + "=?",
            values = arrayOf(playlistId.toString())
        )

    private fun checkExistenceImpl(context: Context, selection: String, values: Array<String>): Boolean =
        context.contentResolver
            .query(mediastoreUriPlaylistsExternal(), arrayOf(), selection, values, null)
            ?.use { it.count > 0 } == true

    /**
     * query playlist file via MediaStore
     */
    private suspend fun queryPlaylists(
        context: Context,
        selection: String?,
        selectionValues: Array<String>?,
        sortOrder: String? = Playlists.DEFAULT_SORT_ORDER,
        withoutPathFilter: Boolean = false,
    ): Cursor? = try {
        withPathFilter(
            context,
            selection = withBasePlaylistFilter { selection },
            selectionValues = selectionValues ?: emptyArray(),
            escape = withoutPathFilter
        ) { actualSelection, actualSelectionValues ->
            queryMediastorePlaylists(
                context = context,
                selection = actualSelection,
                selectionArgs = actualSelectionValues,
                sortOrder = sortOrder,
            )
        }
    } catch (_: SecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        record(context, e, "QueryMediastore")
        null
    }

    private class IconLookup(private val context: Context) : PlaylistLookup {
        private val favoritePlaylistName = context.getString(R.string.playlist_favorites)
        override suspend fun lookupIconRes(mediastoreId: Long, name: String, path: String): Int {
            return when {
                name == favoritePlaylistName                        -> R.drawable.ic_favorite_white_24dp
                PinedPlaylists.isPined(context, mediastoreId, path) -> R.drawable.ic_pin_white_24dp
                else                                                -> R.drawable.ic_file_music_white_24dp
            }
        }
    }

    private fun List<Playlist>.sortAll(context: Context): List<Playlist> =
        sortAll(Setting(context)[Keys.playlistSortMode].data)

    private fun List<Playlist>.sortAll(sortMode: SortMode): List<Playlist> =
        this.sort(sortMode.revert, mediastorePlaylistSortRefKey(sortMode.sortRef))

    /**
     * check a song whether be in a playlist or not
     */
    fun contains(context: Context, volume: String, playlistId: Long, songId: Long): Boolean {
        if (playlistId <= 0) return false
        try {
            val cursor = context.contentResolver.query(
                mediastoreUriPlaylistMembers(volume, playlistId),
                arrayOf(Playlists.Members.AUDIO_ID),
                Playlists.Members.AUDIO_ID + "=?",
                arrayOf(songId.toString()),
                null
            ) ?: return false
            return cursor.use { it.count > 0 }
        } catch (e: UnsupportedOperationException) {
            record(context, e, "PlaylistSong")
            return false
        }
    }

}