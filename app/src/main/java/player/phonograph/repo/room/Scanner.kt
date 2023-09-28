/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room

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
import player.phonograph.model.Song as SongModel

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
            val songs: List<Song>
            if (force) {
                songs = SongLoader.all(context).map(Converters::fromSongModel)
                importFromMediaStore(context, songs)
            } else if (latestSongTimestamp > databaseUpdateTimestamp || databaseUpdateTimestamp == -1L) {
                songs = SongLoader.since(context, databaseUpdateTimestamp).map(Converters::fromSongModel)
                importFromMediaStore(context, songs)
                MusicDatabase.Metadata.lastUpdateTimestamp = currentTimestamp() / 1000
            }
        }
    }

    private fun importFromMediaStore(context: Context, songs: List<Song>) = withNotification(context) {
        val songDataBase = MusicDatabase.songsDataBase
        val relationShipDao = songDataBase.RelationShipDao()
        val artistDao = songDataBase.ArtistDao()
        val albumDao = songDataBase.AlbumDao()
        for (song in songs) {
            // song
            songDataBase.SongDao().override(song)
            debug { Log.d(TAG, "Override Song: ${song.title}") }
            // album
            SongRegistry.registerAlbum(song, albumDao)
            // artist
            SongRegistry.registerArtists(song, artistDao, relationShipDao)
        }
    }

    fun refreshSingleSong(context: Context, songId: Long) =
        refreshSingleSong(context, SongLoader.id(context, songId))

    fun refreshSingleSong(context: Context, song: SongModel) =
        refreshSingleSong(context, Converters.fromSongModel(song))

    fun refreshSingleSong(context: Context, song: Song) {
        scope.launch {
            withNotification(context) {
                val songDataBaseDao = MusicDatabase.songsDataBase.SongDao()
                songDataBaseDao.update(song)
            }
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