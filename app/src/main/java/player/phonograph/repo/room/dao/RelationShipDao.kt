/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.LinkageSongAndArtist
import player.phonograph.repo.room.entity.LinkageSongAndArtist.ArtistRole
import player.phonograph.repo.room.entity.SongEntity
import player.phonograph.util.debug
import player.phonograph.util.text.splitMultiTag
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import android.util.Log

@Dao
interface RelationShipDao {

    @Transaction
    fun register(songEntity: SongEntity) {
        overrideSong(songEntity)
        registerAlbum(songEntity)
        registerArtists(songEntity)
    }


    @Transaction
    fun unregister(songEntity: SongEntity) {
        removeSong(songEntity)
        //todo
    }

    companion object {
        private const val TAG: String = "RelationShipDao"
    }

    private fun registerAlbum(songEntity: SongEntity) {
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
            overrideAlbum(album)
        }
    }


    private fun registerArtists(
        songEntity: SongEntity,
    ) {
        register(songEntity, songEntity.rawArtistName, LinkageSongAndArtist.ROLE_ARTIST)
        register(songEntity, songEntity.composer, LinkageSongAndArtist.ROLE_COMPOSER)
        register(songEntity, songEntity.albumArtistName, LinkageSongAndArtist.ROLE_ALBUM_ARTIST)
    }

    private fun register(
        songEntity: SongEntity,
        raw: String?,
        @ArtistRole role: Int,
    ) {
        if (raw != null) {
            val parsed = splitMultiTag(raw)
            if (parsed.isNotEmpty()) {
                for (name in parsed) {
                    val artist = ArtistEntity(name.hashCode().toLong(), name)
                    overrideArtist(artist)
                    overrideLinkageSongAndArtist(LinkageSongAndArtist(songEntity.id, artist.artistId, role))
                    debug {
                        Log.v(TAG, "* Registering Artist: ${songEntity.title} <--> $name [$role]")
                    }
                }
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun overrideSong(songEntity: SongEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun overrideAlbum(albumEntity: AlbumEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun overrideArtist(artistEntity: ArtistEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun overrideLinkageSongAndArtist(linkage: LinkageSongAndArtist)

    @Delete
    fun removeSong(songEntity: SongEntity)
    @Delete
    fun removeAlbum(albumEntity: AlbumEntity)
    @Delete
    fun removeArtist(artistEntity: ArtistEntity)
    @Delete
    fun removeLinkageSongAndArtist(linkage: LinkageSongAndArtist)
}