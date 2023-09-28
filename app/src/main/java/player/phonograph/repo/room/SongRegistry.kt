/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.repo.room.dao.AlbumDao
import player.phonograph.repo.room.dao.ArtistDao
import player.phonograph.repo.room.dao.RelationShipDao
import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.LinkageSongAndArtist
import player.phonograph.repo.room.entity.LinkageSongAndArtist.ArtistRole
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_ALBUM_ARTIST
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_ARTIST
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_COMPOSER
import player.phonograph.repo.room.entity.SongEntity
import player.phonograph.util.debug
import player.phonograph.util.text.splitMultiTag
import android.util.Log

object SongRegistry {

    fun registerAlbum(
        songEntity: SongEntity,
        albumDao: AlbumDao,
    ) {
        val albumName = songEntity.albumName
        if (albumName != null) {
            val album = AlbumEntity(
                albumId = songEntity.albumId,
                albumName = albumName,
                artistId = 0,
                albumArtistName = songEntity.albumArtistName ?: "",
                year = 0,
                dateModified = 0
            )
            albumDao.override(album)
        }
    }

    fun registerArtists(
        songEntity: SongEntity,
        artistDao: ArtistDao,
        relationShipDao: RelationShipDao,
    ) {
        register(artistDao, relationShipDao, songEntity, songEntity.rawArtistName, ROLE_ARTIST)
        register(artistDao, relationShipDao, songEntity, songEntity.composer, ROLE_COMPOSER)
        register(artistDao, relationShipDao, songEntity, songEntity.albumArtistName, ROLE_ALBUM_ARTIST)
    }

    private fun register(
        artistDao: ArtistDao,
        relationShipDao: RelationShipDao,
        songEntity: SongEntity,
        raw: String?,
        @ArtistRole role: Int,
    ) {
        if (raw != null) {
            val parsed = splitMultiTag(raw)
            if (parsed.isNotEmpty()) {
                for (name in parsed) {
                    val artist = ArtistEntity(name.hashCode().toLong(), name)
                    artistDao.override(artist)
                    relationShipDao.override(LinkageSongAndArtist(songEntity.id, artist.artistId, role))
                    debug {
                        Log.v(TAG, "* Registering Artist: ${songEntity.title} <--> $name [$role]")
                    }
                }
            }
        }
    }

    private const val TAG = "RoomSongRegistry"
}