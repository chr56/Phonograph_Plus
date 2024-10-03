/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.loaders

import legacy.phonograph.MediaStoreCompat.Audio.Playlists
import player.phonograph.model.PlaylistSong
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.internal.BASE_AUDIO_SELECTION
import player.phonograph.util.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.util.mediastoreUriPlaylistMembers
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore.Audio.AudioColumns
import android.util.Log

object PlaylistSongLoader {

    fun songs(context: Context, playlistId: Long): List<PlaylistSong> =
        queryPlaylistSongs(context, playlistId).intoPlaylistSongs(playlistId)

    private fun queryPlaylistSongs(context: Context, playlistId: Long): Cursor? = try {
        context.contentResolver.query(
            mediastoreUriPlaylistMembers(MEDIASTORE_VOLUME_EXTERNAL, playlistId),
            arrayOf(
                Playlists.Members.AUDIO_ID, // 0
                AudioColumns.TITLE, // 1
                AudioColumns.TRACK, // 2
                AudioColumns.YEAR, // 3
                AudioColumns.DURATION, // 4
                AudioColumns.DATA, // 5
                AudioColumns.DATE_ADDED, // 6
                AudioColumns.DATE_MODIFIED, // 7
                AudioColumns.ALBUM_ID, // 8
                AudioColumns.ALBUM, // 9
                AudioColumns.ARTIST_ID, // 10
                AudioColumns.ARTIST, // 11
                AudioColumns.ALBUM_ARTIST, // 12 (hidden api before R)
                AudioColumns.COMPOSER, // 13 (hidden api before R)
                Playlists.Members._ID // 14
            ),
            BASE_AUDIO_SELECTION, null,
            Playlists.Members.DEFAULT_SORT_ORDER
        )
    } catch (e: SecurityException) {
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
            Song(
                id = getLong(0),
                title = getString(1),
                trackNumber = getInt(2),
                year = getInt(3),
                duration = getLong(4),
                data = getString(5),
                dateAdded = getLong(6),
                dateModified = getLong(7),
                albumId = getLong(8),
                albumName = getString(9),
                artistId = getLong(10),
                artistName = getString(11),
                albumArtistName = getString(12),
                composer = getString(13),
            ),
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
            Log.w("PlaylistSong", "Failed to check playlistId $playlistId", e)
            return false
        }
    }
}
