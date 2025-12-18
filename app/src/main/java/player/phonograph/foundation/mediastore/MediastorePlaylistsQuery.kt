/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.foundation.mediastore

import player.phonograph.foundation.compat.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.foundation.compat.MediaStoreCompat.Audio.Playlists
import player.phonograph.foundation.compat.MediaStoreCompat.Audio.PlaylistsColumns
import player.phonograph.model.PlaylistSong
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import android.content.Context
import android.database.Cursor
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.provider.BaseColumns
import android.provider.MediaStore


val BASE_PLAYLIST_PROJECTION_Q
    get() = arrayOf(
        BaseColumns._ID, // 0
        PlaylistsColumns.NAME, // 1
        PlaylistsColumns.DATA, // 2
        PlaylistsColumns.DATE_ADDED, // 3
        PlaylistsColumns.DATE_MODIFIED, // 4
        MediaStore.MediaColumns.VOLUME_NAME, // 5
    )

val BASE_PLAYLIST_PROJECTION
    get() = arrayOf(
        BaseColumns._ID, // 0
        PlaylistsColumns.NAME, // 1
        PlaylistsColumns.DATA, // 2
        PlaylistsColumns.DATE_ADDED, // 3
        PlaylistsColumns.DATE_MODIFIED, // 4
    )

val BASE_PLAYLIST_SONG_PROJECTION =
    arrayOf(Playlists.Members.AUDIO_ID) + BASE_SONG_PROJECTION.drop(1) + arrayOf(Playlists.Members._ID)


fun queryMediastorePlaylists(
    context: Context,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = Playlists.DEFAULT_SORT_ORDER,
): Cursor? = context.contentResolver.query(
    mediastoreUriPlaylistsExternal(),
    if (SDK_INT > Q) BASE_PLAYLIST_PROJECTION_Q else BASE_PLAYLIST_PROJECTION,
    selection,
    selectionArgs,
    sortOrder,
)


fun queryMediastorePlaylistSongs(
    context: Context,
    playlistId: Long,
    sortOrder: String? = null,
): Cursor? = context.contentResolver.query(
    mediastoreUriPlaylistMembersExternal(playlistId),
    BASE_PLAYLIST_SONG_PROJECTION,
    BASE_AUDIO_SELECTION,
    null,
    sortOrder
)


interface PlaylistLookup {
    suspend fun lookupIconRes(mediastoreId: Long, name: String, path: String): Int
}

/**
 * read cursor as [Playlist]
 *
 * **Requirement:**
 * - [cursor] is queried from **[BASE_PLAYLIST_PROJECTION]**
 * - [cursor] is **not empty**
 *
 */
suspend fun readPlaylist(
    cursor: Cursor,
    iconLookup: PlaylistLookup,
): Playlist {
    val mediastoreId = cursor.getLong(0)
    val name = cursor.getString(1)
    val path = cursor.getString(2)
    val dateAdded = cursor.getLong(3)
    val dateModified = cursor.getLong(4)
    val storageVolume = if (SDK_INT > Q) cursor.getString(5) else MEDIASTORE_VOLUME_EXTERNAL

    return Playlist(
        name = name,
        location = FilePlaylistLocation(
            path = path,
            storageVolume = storageVolume,
            mediastoreId = mediastoreId
        ),
        dateAdded = dateAdded,
        dateModified = dateModified,
        iconRes = iconLookup.lookupIconRes(mediastoreId, name, path)
    )
}

/**
 * consume cursor (read & close) and convert into first FilePlaylist
 */
suspend fun Cursor?.intoFirstPlaylist(lookup: PlaylistLookup): Playlist? {
    return this?.use {
        if (moveToFirst()) readPlaylist(this, lookup) else null
    }
}


/**
 * consume cursor (read & close) and convert into FilePlaylist list
 */
suspend fun Cursor?.intoPlaylists(lookup: PlaylistLookup): List<Playlist> =
    this?.use {
        val filePlaylists = mutableListOf<Playlist>()
        if (moveToFirst()) {
            do {
                filePlaylists.add(readPlaylist(this, lookup))
            } while (moveToNext())
        }
        filePlaylists
    } ?: emptyList()



fun Cursor.readPlaylistSong(playlistId: Long): PlaylistSong =
    PlaylistSong(
        readSong(this),
        playlistId = playlistId,
        idInPlayList = getLong(14),
    )

fun Cursor?.intoPlaylistSongs(playlistId: Long): List<PlaylistSong> =
    this?.use {
        val songs = mutableListOf<PlaylistSong>()
        if (moveToFirst()) {
            do {
                songs.add(this.readPlaylistSong(playlistId))
            } while (moveToNext())
        }
        songs
    } ?: emptyList()



const val BASE_PLAYLIST_SELECTION = "${PlaylistsColumns.NAME} != '' "

inline fun withBasePlaylistFilter(block: () -> String?): String {
    val selection = block()
    return if (selection.isNullOrBlank()) {
        BASE_PLAYLIST_SELECTION
    } else {
        "$BASE_PLAYLIST_SELECTION AND $selection "
    }
}

