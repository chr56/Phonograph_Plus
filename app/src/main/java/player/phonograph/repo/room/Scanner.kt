/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.App
import player.phonograph.notification.DatabaseUpdateNotification
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.repo.room.entity.Song
import player.phonograph.util.debug
import player.phonograph.util.text.currentTimestamp
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Scanner {

    private const val TAG = "DatabaseScanner"
    private val scope by lazy { CoroutineScope(Dispatchers.IO) }

    fun refreshDatabase(context: Context, force: Boolean = false) {
        Log.i(TAG, "Start refreshing")
        var latestSongTimestamp = -1L
        var databaseUpdateTimestamp = -1L

        // check latest music files
        val latestSong = SongLoader.latest(context)
        if (latestSong != null && latestSong.dateModified > 0) latestSongTimestamp = latestSong.dateModified
        // check database timestamps
        databaseUpdateTimestamp = MusicDatabase.Metadata.lastUpdateTimestamp

        debug {
            Log.i(TAG, "latestSongTimestamp    :$latestSongTimestamp")
            Log.i(TAG, "databaseUpdateTimestamp:$databaseUpdateTimestamp")
        }

        // compare
        scope.launch {
            if (force) {
                val songs =
                    SongLoader.all(context).map(SongConverter::fromSongModel)
                importFromMediaStore(context, songs)
            } else if (latestSongTimestamp > databaseUpdateTimestamp || databaseUpdateTimestamp == -1L) {
                val songs: List<Song> =
                    SongLoader.since(context, databaseUpdateTimestamp).map(SongConverter::fromSongModel)
                importFromMediaStore(context, songs)
                MusicDatabase.Metadata.lastUpdateTimestamp = currentTimestamp() / 1000
            }
        }
    }

    private fun importFromMediaStore(context: Context, songs: List<Song>) = withNotification(context) {
        val songDataBase = MusicDatabase.songsDataBase
        for (song in songs) {
            // song
            songDataBase.SongDao().override(song)
            debug { Log.d(TAG, "Override Song: ${song.title}") }
            // album
            songDataBase.AlbumDao().override(SongMarker.getAlbum(song))
            // artist
            val artistSongsDao = MusicDatabase.songsDataBase.ArtistSongsDao()
            val artistDao = MusicDatabase.songsDataBase.ArtistDao()
            SongRegistry.registerArtists(song, artistDao, artistSongsDao)
        }
    }

    fun refreshSingleSong(context: Context?, songId: Long) {
        refreshSingleSong(context, SongLoader.id(context ?: App.instance, songId))
    }

    fun refreshSingleSong(context: Context?, song: player.phonograph.model.Song) {
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

    private inline fun withNotification(context: Context, block: (Context) -> Unit) {
        Log.d(TAG, "Start importing")
        DatabaseUpdateNotification.send(context)

        block(context)

        Log.d(TAG, "End importing")
        DatabaseUpdateNotification.cancel(context)
    }

}