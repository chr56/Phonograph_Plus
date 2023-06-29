/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.loaders

import legacy.phonograph.MediaStoreCompat.Audio.Playlists
import player.phonograph.model.PlaylistSong
import player.phonograph.repo.mediastore.internal.BASE_AUDIO_SELECTION
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore.Audio.AudioColumns

object PlaylistSongLoader {

    fun getPlaylistSongList(context: Context, playlistId: Long): List<PlaylistSong> {

        val songs: MutableList<PlaylistSong> = ArrayList()
        makeCursor(context, playlistId)?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    songs.add(getPlaylistSongFromCursorImpl(cursor, playlistId))
                } while (cursor.moveToNext())
            }
        }

        return songs
    }

    private fun getPlaylistSongFromCursorImpl(cursor: Cursor, playlistId: Long): PlaylistSong =
        PlaylistSong(
            id = cursor.getLong(0),
            title = cursor.getString(1),
            trackNumber = cursor.getInt(2),
            year = cursor.getInt(3),
            duration = cursor.getLong(4),
            data = cursor.getString(5),
            dateAdded = cursor.getLong(6),
            dateModified = cursor.getLong(7),
            albumId = cursor.getLong(8),
            albumName = cursor.getString(9),
            artistId = cursor.getLong(10),
            artistName = cursor.getString(11),
            albumArtistName = cursor.getString(12),
            composer = cursor.getString(13),
            playlistId = playlistId,
            idInPlayList = cursor.getLong(14),
        )

    private fun makeCursor(context: Context, playlistId: Long): Cursor? {
        return try {
            context.contentResolver.query(
                Playlists.Members.getContentUri("external", playlistId),
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
    }


    /**
     * check a song whether be in a playlist or not
     */
    fun doesPlaylistContain(context: Context, playlistId: Long, songId: Long): Boolean {
        val cursor = context.contentResolver.query(
            PlaylistLoader.idToMediastoreUri(playlistId),
            arrayOf(Playlists.Members.AUDIO_ID),
            Playlists.Members.AUDIO_ID + "=?",
            arrayOf(songId.toString()),
            null
        ) ?: return false
        return cursor.use { it.count > 0 }
    }
}
