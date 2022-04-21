/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import player.phonograph.model.PlaylistSong

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
            playlistId = playlistId,
            idInPlayList = cursor.getLong(12),
        )

    @Suppress("DEPRECATION")
    private fun makeCursor(context: Context, playlistId: Long): Cursor? {
        return try {
            context.contentResolver.query(
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                arrayOf(
                    MediaStore.Audio.Playlists.Members.AUDIO_ID, // 0
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
                    MediaStore.Audio.Playlists.Members._ID // 12
                ),
                MediaStoreUtil.SongConst.BASE_AUDIO_SELECTION, null,
                MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER
            )
        } catch (e: SecurityException) {
            null
        }
    }
}
