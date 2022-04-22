/*
 *  Copyright (c) 2022~2023 chr_56
 */

@file:Suppress("unused")

package player.phonograph.repo.room

import player.phonograph.App
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.util.debug
import player.phonograph.util.text.splitMultiTag
import player.phonograph.util.text.currentTimestamp
import androidx.room.TypeConverter
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.phonograph.model.Song as OldSongModel

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
        return OldSongModel(
            id = song.id,
            title = song.title ?: "UNKNOWN",
            trackNumber = song.trackNumber,
            year = song.year,
            duration = song.duration,
            data = song.path,
            dateAdded = song.dateModified,
            dateModified = song.dateModified,
            albumId = song.albumId,
            albumName = song.albumName,
            artistId = song.artistId,
            artistName = song.artistName,
            "",
            ""
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
        return Artist(song.artistId, song.artistName ?: "")
    }
}

object SongRegistry {
    fun registerArtists(song: Song) {
        val raw = song.artistName
        if (raw != null) {
            val artistSongsDao = MusicDatabase.songsDataBase.ArtistSongsDao()
            val artistDao = MusicDatabase.songsDataBase.ArtistDao()

            val parsed = splitMultiTag(raw)

            if (parsed != null) {
                for (name in parsed) {
                    val artist = Artist(name.hashCode().toLong(), name)

                    artistDao.override(artist)
                    artistSongsDao.override(SongAndArtistLinkage(song.id, artist.artistId))

                    Log.v(TAG, "::artist was registered: ${song.title}<->$name")
                }
            } else {
                Log.v(TAG, "no artist in Song ${song.title}")
            }
        } else {
            Log.v(TAG, "no artist in Song ${song.title}")
        }
    }
}

private const val TAG = "RoomDatabase"
private val scope = CoroutineScope(Dispatchers.IO)

object Refresher2 {

    fun importFromMediaStore(context: Context, sinceTimestamp: Long, callbacks: (() -> Unit)?) {
        Log.i(TAG, "Start importing")

        scope.launch {
            // DatabaseUpdateNotification.sendNotification(context.getString(R.string.updating_database))
            val songs: List<Song> = SongLoader.since(context, sinceTimestamp).map(SongConverter::fromSongModel)
            val songDataBase = MusicDatabase.songsDataBase

            for (song in songs) {
                // song
                songDataBase.SongDao().override(song)
                Log.d(TAG, "Override Song: ${song.title}")
                // album
                songDataBase.AlbumDao().override(SongMarker.getAlbum(song))
                // artist
                SongRegistry.registerArtists(song)
            }

            Log.i(TAG, "End importing")
            // DatabaseUpdateNotification.cancelNotification()
            callbacks?.let { it() }
        }
    }

    fun refreshSingleSong(context: Context?, songId: Long) {
        refreshSingleSong(context, SongLoader.id(context ?: App.instance, songId))
    }

    fun refreshSingleSong(context: Context?, song: OldSongModel) {
        scope.launch {
            val songDataBaseDao = MusicDatabase.songsDataBase.SongDao()
            songDataBaseDao.update(SongConverter.fromSongModel(song))
        }
    }

    fun refreshSingleSong(context: Context?, song: Song) {
        scope.launch {
            val songDataBaseDao = MusicDatabase.songsDataBase.SongDao()
            songDataBaseDao.update(song)
        }
    }

}
