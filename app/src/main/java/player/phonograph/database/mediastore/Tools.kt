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

// todo remove
object SongConverter {
    @TypeConverter
    fun fromSongModel(song: player.phonograph.model.Song): Song {
        return Song(
            song.id,
            song.data,
            song.title,
            song.year,
            song.duration,
            song.dateModified,
            song.albumId,
            song.albumName,
            song.artistId,
            song.artistName,
            song.trackNumber
        )
    }

    @TypeConverter
    fun toSongModel(song: Song): player.phonograph.model.Song {
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
            val songDataBaseDao = MusicDatabase.songsDataBase.SongDao()

            if (songs != null) {
                for (song in songs.listIterator()) {
                    songDataBaseDao.override(song)
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
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(6),
                        cursor.getInt(11),
                        cursor.getLong(12),
                        cursor.getLong(5),
                        cursor.getLong(7),
                        cursor.getString(8),
                        cursor.getLong(9),
                        cursor.getString(10),
                        cursor.getInt(13),
                        // todo
                    )
                )
            } while (cursor.moveToNext())
            cursor.close()

            return songs
        } else
            return null
    }
}
