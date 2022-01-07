/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

@file:Suppress("unused")

package player.phonograph.database.mediastore

import android.content.Context
import android.provider.BaseColumns
import android.provider.MediaStore
import android.util.Log
import androidx.room.TypeConverter
import player.phonograph.App
import player.phonograph.R
import player.phonograph.helper.SortOrder
import player.phonograph.notification.DatabaseUpdateNotification
import player.phonograph.provider.BlacklistStore
import player.phonograph.util.MediaStoreUtil
import player.phonograph.database.mediastore.Song as Song
import player.phonograph.model.Song as OldSongModel

// todo remove
object SongConverter {
    @TypeConverter
    fun fromSongModel(song: OldSongModel): Song {
        // todo
        return Song(
            id = song.id,
            path = song.data,
            size = 0,
            displayName = "",
            dateAdded = song.dateModified,
            dateModified = song.dateModified,
            title = song.title,
            albumId = song.albumId,
            albumName = song.albumName,
            artistId = song.artistId,
            artistName = song.artistName,
            year = song.year,
            duration = song.duration,
            trackNumber = song.trackNumber
        )
    }

    @TypeConverter
    fun toSongModel(song: Song): OldSongModel {
        return player.phonograph.model.Song(
            song.id,
            song.title,
            song.trackNumber,
            song.year,
            song.duration,
            song.path,
            song.dateModified,
            song.albumId,
            song.albumName,
            song.artistId,
            song.artistName
        )
    }
}

object SongMarker {

    @TypeConverter
    fun getAlbum(song: Song): Album {
        return Album(song.albumId, song.albumName)
    }
    @TypeConverter
    fun getArtist(song: Song): Artist {
        return Artist(song.artistId, song.artistName)
    }
}

object Refresher {
    const val TAG = "RoomDatabase"

    fun importFromMediaStore(context: Context, callbacks: (() -> Unit)?) {
        Log.i(TAG, "Start importing")

        App.instance.threadPoolExecutors.execute {
            DatabaseUpdateNotification.sendNotification(context.getString(R.string.updating_database))
            val songs: List<Song>? = getAllSongs(context)
            val songDataBase = MusicDatabase.songsDataBase

            if (songs != null) {
                for (song in songs.listIterator()) {
                    songDataBase.SongDao().override(song)
                    songDataBase.AlbumDao().override(SongMarker.getAlbum(song))
                    Log.d(TAG, "Add Song: ${song.title}")
                }
                MusicDatabase.songsDataBase.lastUpdateTimestamp = getLastSong(context).dateModified
            } else Log.e(TAG, "No songs?")

            Log.i(TAG, "End importing")
            DatabaseUpdateNotification.cancelNotification()
            callbacks?.let { it() }
        }
    }

    fun refreshSingleSong(context: Context?, songId: Long) {
        refreshSingleSong(context, MediaStoreUtil.getSong(context ?: App.instance, songId))
    }

    fun refreshSingleSong(context: Context?, song: player.phonograph.model.Song) {
        App.instance.threadPoolExecutors.execute {
            val songDataBaseDao = MusicDatabase.songsDataBase.SongDao()
            songDataBaseDao.update(SongConverter.fromSongModel(song))
        }
    }

    fun refreshSingleSong(context: Context?, song: Song) {
        App.instance.threadPoolExecutors.execute {
            val songDataBaseDao = MusicDatabase.songsDataBase.SongDao()
            songDataBaseDao.update(song)
        }
    }

    fun getLastSong(context: Context): player.phonograph.model.Song {

        return MediaStoreUtil.getSong(
            MediaStoreUtil.querySongs(
                context,
                null,
                null,
                SortOrder.SongSortOrder.SONG_DATE_MODIFIED_REVERT
            )
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    val BASE_PROJECTION = arrayOf(
        BaseColumns._ID, // 0
        MediaStore.MediaColumns.DATA, // 1
        MediaStore.MediaColumns.SIZE, // 2
        MediaStore.MediaColumns.DISPLAY_NAME, // 3
        MediaStore.MediaColumns.DATE_ADDED, // 4
        MediaStore.MediaColumns.DATE_MODIFIED, // 5
        MediaStore.Audio.AudioColumns.TITLE, // 6
        MediaStore.Audio.AudioColumns.ALBUM_ID, // 7
        MediaStore.Audio.AudioColumns.ALBUM, // 8
        MediaStore.Audio.AudioColumns.ARTIST_ID, // 9
        MediaStore.Audio.AudioColumns.ARTIST, // 10
        MediaStore.Audio.AudioColumns.YEAR, // 11
        MediaStore.Audio.AudioColumns.DURATION, // 12
        MediaStore.Audio.AudioColumns.TRACK, // 13
//            For Android 10 todo implement
//            MediaStore.Audio.AudioColumns.DISC_NUMBER,
//            MediaStore.Audio.AudioColumns.GENRE,
    )

    private fun getAllSongs(c: Context?): List<Song>? {
        val context = c ?: App.instance

        val paths: List<String> = BlacklistStore.getInstance(context).paths

        val selection: String =
            if (paths.isNotEmpty()) {
                var s = ""
                for (i in paths) {
                    s += " AND ${MediaStore.Audio.AudioColumns.DATA} NOT LIKE ?"
                }
                s
            } else ""

        val selectionValues: Array<String>? =
            if (paths.isNotEmpty()) {
                Array(paths.size) { index -> paths[index] }
            } else null

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            BASE_PROJECTION,
            "${MediaStore.Audio.AudioColumns.IS_MUSIC} = 1 AND ${MediaStore.Audio.AudioColumns.TITLE} != '' $selection",
            selectionValues,
            MediaStore.Audio.Media.DATE_MODIFIED + " DESC"
        )

        if (cursor != null && cursor.moveToFirst()) {
            val songs: MutableList<Song> = ArrayList()
            do {
                songs.add(
                    Song(
                        id = cursor.getLong(0),
                        path = cursor.getString(1),
                        size = cursor.getLong(2),
                        displayName = cursor.getString(3),
                        dateAdded = cursor.getLong(4),
                        dateModified = cursor.getLong(5),
                        title = cursor.getString(6),
                        albumId = cursor.getLong(7),
                        albumName = cursor.getString(8),
                        artistId = cursor.getLong(9),
                        artistName = cursor.getString(10),
                        year = cursor.getInt(11),
                        duration = cursor.getLong(12),
                        trackNumber = cursor.getInt(13)
                    )
                )
            } while (cursor.moveToNext())
            cursor.close()

            return songs
        } else
            return null
    }
}
