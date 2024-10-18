/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.loaders

import legacy.phonograph.MediaStoreCompat.Audio.Playlists
import player.phonograph.model.PlaylistSong
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.PlaylistLocation
import player.phonograph.repo.mediastore.internal.BASE_AUDIO_SELECTION
import player.phonograph.repo.mediastore.internal.BASE_SONG_PROJECTION
import player.phonograph.repo.mediastore.internal.readSong
import player.phonograph.util.MEDIASTORE_VOLUME_EXTERNAL
import player.phonograph.util.mediastoreUriPlaylistMembers
import android.content.Context
import android.database.Cursor
import android.util.Log

object PlaylistSongLoader {

    fun songs(context: Context, playlistId: Long): List<PlaylistSong> =
        queryPlaylistSongs(context, playlistId).intoPlaylistSongs(playlistId)

    private val PROJECTION =
        arrayOf(Playlists.Members.AUDIO_ID) + BASE_SONG_PROJECTION.drop(1) + arrayOf(Playlists.Members._ID)

    private fun queryPlaylistSongs(context: Context, playlistId: Long): Cursor? = try {
        context.contentResolver.query(
            mediastoreUriPlaylistMembers(MEDIASTORE_VOLUME_EXTERNAL, playlistId),
            PROJECTION,
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
            readSong(this),
            playlistId = playlistId,
            idInPlayList = getLong(14),
        )

    fun contains(context: Context, location: PlaylistLocation, songId: Long): Boolean =
        if (location is FilePlaylistLocation) {
            contains(context, location.storageVolume, location.mediastoreId, songId)
        } else {
            false
        }

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
