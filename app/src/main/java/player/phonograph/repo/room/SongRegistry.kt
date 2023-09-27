/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.repo.room.entity.Artist
import player.phonograph.repo.room.entity.Song
import player.phonograph.repo.room.entity.SongAndArtistLinkage
import player.phonograph.util.debug
import player.phonograph.util.text.splitMultiTag
import android.util.Log

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

                    debug { Log.v(TAG, "::artist was registered: ${song.title}<->$name") }
                }
            } else {
                debug { Log.v(TAG, "no artist in Song ${song.title}") }
            }
        } else {
            debug { Log.v(TAG, "no artist in Song ${song.title}") }
        }
    }
    private const val TAG = "RoomSongRegistry"
}