/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.database.mediastore

import android.content.Context
import android.util.Log
import androidx.room.TypeConverter
import player.phonograph.App
import player.phonograph.R
import player.phonograph.helper.SortOrder
import player.phonograph.notification.DatabaseUpdateNotification
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

    fun importFromMediaStore(context: Context, callbacks: (() -> Unit)?) {
        Log.i("RoomDatabase", "Start importing")

        App.instance.threadPoolExecutors.execute {
            DatabaseUpdateNotification.sendNotification(context.getString(R.string.updating_database))
            val songs = MediaStoreUtil.getAllSongs(context)
            val songDataBaseDao = MusicDatabase.songsDataBase.SongDao()
            for (song in songs.listIterator()) {
                song?.let {
                    songDataBaseDao.override(SongConverter.fromSongModel(it))
                    Log.d("RoomDatabase", "Add Song:${it.title}")
                }
            }
            MusicDatabase.songsDataBase.lastUpdateTimestamp = getLastSong(context).dateModified
            Log.i("RoomDatabase", "End importing")
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
}
