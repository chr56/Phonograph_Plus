/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.R
import player.phonograph.foundation.compat.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.foundation.compat.MediaStoreCompat.Audio.Playlists
import player.phonograph.foundation.compat.MediaStoreCompat.Audio.PlaylistsColumns
import player.phonograph.foundation.error.record
import player.phonograph.foundation.mediastore.BASE_AUDIO_SELECTION
import player.phonograph.foundation.mediastore.BASE_SONG_PROJECTION
import player.phonograph.foundation.mediastore.mediastoreUriPlaylistMembers
import player.phonograph.foundation.mediastore.mediastoreUriPlaylistMembersExternal
import player.phonograph.foundation.mediastore.mediastoreUriPlaylistsExternal
import player.phonograph.foundation.mediastore.readSong
import player.phonograph.model.PlaylistSong
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.PlaylistLocation
import player.phonograph.model.repo.loader.IPlaylists
import player.phonograph.model.sort.SortMode
import player.phonograph.repo.loader.PinedPlaylists
import player.phonograph.repo.mediastore.internal.SQLWhereClause
import player.phonograph.repo.mediastore.internal.withBasePlaylistFilter
import player.phonograph.repo.mediastore.internal.withPathFilter
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.sort
import android.content.Context
import android.database.Cursor
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.provider.BaseColumns
import android.provider.MediaStore

object MediaStorePlaylists : IPlaylists {

    override suspend fun all(context: Context): List<Playlist> =
        queryPlaylists(context, null, null).intoPlaylists(context).sortAll(context)

    suspend fun id(context: Context, id: Long): Playlist? =
        queryPlaylists(context, BaseColumns._ID + "=?", arrayOf(id.toString())).intoFirstPlaylist(context)

    override suspend fun of(context: Context, location: PlaylistLocation): Playlist? =
        id(context, (location as FilePlaylistLocation).mediastoreId)

    fun songs(context: Context, id: Long): List<PlaylistSong> =
        queryPlaylistSongs(context, id).intoPlaylistSongs(id)

    override suspend fun songs(context: Context, location: PlaylistLocation): List<PlaylistSong> =
        songs(context, (location as FilePlaylistLocation).mediastoreId)

    override suspend fun contains(context: Context, location: PlaylistLocation, songId: Long): Boolean =
        if (location is FilePlaylistLocation) {
            contains(context, location.storageVolume, location.mediastoreId, songId)
        } else {
            false
        }

    override suspend fun named(context: Context, name: String): Playlist? =
        queryPlaylists(context, PlaylistsColumns.NAME + "=?", arrayOf(name)).intoFirstPlaylist(context)

    override suspend fun exists(context: Context, location: PlaylistLocation): Boolean =
        checkExistence(context, (location as FilePlaylistLocation).mediastoreId)

    override suspend fun searchByName(context: Context, query: String): List<Playlist> =
        queryPlaylists(
            context, "${PlaylistsColumns.NAME} LIKE ?", arrayOf("%$query%")
        ).intoPlaylists(context)

    suspend fun searchByPath(context: Context, path: String): Playlist? =
        queryPlaylists(
            context, "${PlaylistsColumns.DATA} = ?", arrayOf(path)
        ).intoFirstPlaylist(context)



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
            mediastoreUriPlaylistsExternal(),
            if (SDK_INT > Q) BASE_PLAYLIST_PROJECTION_Q else BASE_PLAYLIST_PROJECTION,
            actual.selection,
            actual.selectionValues,
            Playlists.DEFAULT_SORT_ORDER
        )
    }

    private val BASE_PLAYLIST_PROJECTION_Q
        get() = arrayOf(
            BaseColumns._ID, // 0
            PlaylistsColumns.NAME, // 1
            PlaylistsColumns.DATA, // 2
            PlaylistsColumns.DATE_ADDED, // 3
            PlaylistsColumns.DATE_MODIFIED, // 4
            MediaStore.MediaColumns.VOLUME_NAME, // 5
        )

    private val BASE_PLAYLIST_PROJECTION
        get() = arrayOf(
            BaseColumns._ID, // 0
            PlaylistsColumns.NAME, // 1
            PlaylistsColumns.DATA, // 2
            PlaylistsColumns.DATE_ADDED, // 3
            PlaylistsColumns.DATE_MODIFIED, // 4
        )


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

    private fun List<Playlist>.sortAll(context: Context): List<Playlist> =
        sortAll(Setting(context)[Keys.playlistSortMode].data)

    private fun List<Playlist>.sortAll(sortMode: SortMode): List<Playlist> =
        this.sort(sortMode.revert, mediastorePlaylistSortRefKey(sortMode.sortRef))

    private fun queryPlaylistSongs(context: Context, playlistId: Long): Cursor? = try {
        context.contentResolver.query(
            mediastoreUriPlaylistMembersExternal(playlistId),
            arrayOf(Playlists.Members.AUDIO_ID) + BASE_SONG_PROJECTION.drop(1) + arrayOf(Playlists.Members._ID),
            BASE_AUDIO_SELECTION, null,
            Playlists.Members.DEFAULT_SORT_ORDER
        )
    } catch (_: SecurityException) {
        null
    }

    private fun Cursor?.intoPlaylistSongs(playlistId: Long): List<PlaylistSong> =
        this?.use {
            val songs = mutableListOf<PlaylistSong>()
            if (moveToFirst()) {
                do {
                    songs.add(this.readPlaylistSong(playlistId))
                } while (moveToNext())
            }
            songs
        } ?: emptyList()

    private fun Cursor.readPlaylistSong(playlistId: Long): PlaylistSong =
        PlaylistSong(
            readSong(this),
            playlistId = playlistId,
            idInPlayList = getLong(14),
        )

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