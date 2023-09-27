/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.repo.room.dao.AlbumDAO
import player.phonograph.repo.room.dao.ArtistDAO
import player.phonograph.repo.room.dao.ArtistSongDAO
import player.phonograph.repo.room.entity.Album
import player.phonograph.repo.room.entity.Artist
import player.phonograph.repo.room.entity.Song
import player.phonograph.repo.room.entity.SongAndArtistLinkage
import player.phonograph.repo.room.entity.SongAndArtistLinkage.ArtistRole
import player.phonograph.repo.room.entity.SongAndArtistLinkage.Companion.ROLE_ALBUM_ARTIST
import player.phonograph.repo.room.entity.SongAndArtistLinkage.Companion.ROLE_ARTIST
import player.phonograph.repo.room.entity.SongAndArtistLinkage.Companion.ROLE_COMPOSER
import player.phonograph.util.debug
import player.phonograph.util.text.splitMultiTag
import android.util.Log

object SongRegistry {

    fun registerAlbum(
        song: Song,
        albumDao: AlbumDAO,
    ) {
        val albumName = song.albumName
        if (albumName != null) {
            val album = Album(song.albumId, albumName, song.albumArtistName)
            albumDao.override(album)
        }
    }

    fun registerArtists(
        song: Song,
        artistDao: ArtistDAO,
        artistSongsDao: ArtistSongDAO,
    ) {
        register(artistDao, artistSongsDao, song, song.artistName, ROLE_ARTIST)
        register(artistDao, artistSongsDao, song, song.composer, ROLE_COMPOSER)
        register(artistDao, artistSongsDao, song, song.albumArtistName, ROLE_ALBUM_ARTIST)
    }

    private fun register(
        artistDao: ArtistDAO,
        artistSongsDao: ArtistSongDAO,
        song: Song,
        raw: String?,
        @ArtistRole role: Int,
    ) {
        if (raw != null) {
            val parsed = splitMultiTag(raw)
            if (parsed.isNotEmpty()) {
                for (name in parsed) {
                    val artist = Artist(name.hashCode().toLong(), name)
                    artistDao.override(artist)
                    artistSongsDao.override(SongAndArtistLinkage(song.id, artist.artistId, role))
                    debug {
                        Log.v(TAG, "* Registering Artist: ${song.title} <--> $name [$role]")
                    }
                }
            }
        }
    }

    private const val TAG = "RoomSongRegistry"
}