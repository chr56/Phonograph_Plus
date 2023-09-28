/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.repo.room.dao.AlbumDao
import player.phonograph.repo.room.dao.ArtistDao
import player.phonograph.repo.room.dao.RelationShipDao
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
        albumDao: AlbumDao,
    ) {
        val albumName = song.albumName
        if (albumName != null) {
            val album = Album(
                albumId = song.albumId,
                albumName = albumName,
                artistId = 0,
                albumArtistName = song.albumArtistName ?: "",
                year = 0,
                dateModified = 0
            )
            albumDao.override(album)
        }
    }

    fun registerArtists(
        song: Song,
        artistDao: ArtistDao,
        relationShipDao: RelationShipDao,
    ) {
        register(artistDao, relationShipDao, song, song.artistName, ROLE_ARTIST)
        register(artistDao, relationShipDao, song, song.composer, ROLE_COMPOSER)
        register(artistDao, relationShipDao, song, song.albumArtistName, ROLE_ALBUM_ARTIST)
    }

    private fun register(
        artistDao: ArtistDao,
        relationShipDao: RelationShipDao,
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
                    relationShipDao.override(SongAndArtistLinkage(song.id, artist.artistId, role))
                    debug {
                        Log.v(TAG, "* Registering Artist: ${song.title} <--> $name [$role]")
                    }
                }
            }
        }
    }

    private const val TAG = "RoomSongRegistry"
}