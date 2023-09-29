/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.LinkageAlbumAndArtist
import player.phonograph.repo.room.entity.LinkageSongAndArtist
import player.phonograph.repo.room.entity.LinkageSongAndArtist.ArtistRole
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_ALBUM_ARTIST
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_ARTIST
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_COMPOSER
import player.phonograph.repo.room.entity.SongEntity
import player.phonograph.util.debug
import player.phonograph.util.text.splitMultiTag
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import android.util.Log
import kotlin.math.max

@Dao
abstract class RelationShipDao {

    @Transaction
    open fun register(songEntity: SongEntity, albumDao: AlbumDao, artistDao: ArtistDao) {
        overrideSong(songEntity)
        val registerArtists = registerArtists(songEntity, artistDao)
        registerAlbum(songEntity, registerArtists, albumDao)
    }


    @Transaction
    open fun unregister(songEntity: SongEntity, albumDao: AlbumDao, artistDao: ArtistDao) {
        removeSong(songEntity)
        //todo
    }

    companion object {
        private const val TAG: String = "RelationShipDao"
    }

    private fun registerAlbum(songEntity: SongEntity, registerArtists: Collection<ArtistEntity>, albumDao: AlbumDao) {
        val albumName = songEntity.albumName
        if (albumName != null) {
            val artists = registerArtists.toSet()
            val albumArtistName = songEntity.albumArtistName
            val old = albumDao.id(songEntity.albumId)
            val targetArtist =
                artists.firstOrNull { it.artistName == albumArtistName } ?: artists.firstOrNull()
            @Suppress("IfThenToElvis")
            val album =
                if (old != null) {
                    // update
                    old.copy(
                        artistId = targetArtist?.artistId ?: 0,
                        albumArtistName = targetArtist?.artistName ?: "",
                        dateModified = max(old.dateModified, songEntity.dateModified),
                        year = max(old.year, songEntity.year),
                        songCount = old.songCount + 1,
                    )
                } else {
                    // new
                    AlbumEntity(
                        albumId = songEntity.albumId,
                        albumName = albumName,
                        artistId = targetArtist?.artistId ?: 0,
                        albumArtistName = targetArtist?.artistName ?: "",
                        year = songEntity.year,
                        dateModified = songEntity.dateModified,
                        songCount = 1,
                    )
                }
            overrideAlbum(album)
        }
    }


    private fun registerArtists(
        songEntity: SongEntity,
        artistDao: ArtistDao,
    ): Collection<ArtistEntity> {
        val r = registerArtist(songEntity, artistDao, songEntity.rawArtistName, ROLE_ARTIST)
        val c = registerArtist(songEntity, artistDao, songEntity.composer, ROLE_COMPOSER)
        val a = registerArtist(songEntity, artistDao, songEntity.albumArtistName, ROLE_ALBUM_ARTIST)
        return r + c + a
    }

    /**
     * @return registered artists
     */
    private fun registerArtist(
        songEntity: SongEntity,
        artistDao: ArtistDao,
        raw: String?,
        @ArtistRole role: Int,
    ): Collection<ArtistEntity> =
        if (raw != null) {
            val parsed = splitMultiTag(raw)
            if (parsed.isNotEmpty()) {
                parsed.map { name ->
                    val old = artistDao.named(name)
                    @Suppress("IfThenToElvis")
                    if (old != null) {
                        // update
                        old.copy(
                            songCount = old.songCount + 1
                        )
                    } else {
                        ArtistEntity(name.hashCode().toLong(), name, albumCount = 1, songCount = 1)
                    }
                }.map { artist ->
                    overrideArtist(artist)
                    overrideLinkageSongAndArtist(LinkageSongAndArtist(songEntity.id, artist.artistId, role))
                    debug {
                        Log.v(TAG, "* Registering Artist: ${songEntity.title} <--> ${artist.artistName} [$role]")
                    }
                    artist
                }
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun overrideSong(songEntity: SongEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun overrideAlbum(albumEntity: AlbumEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun overrideArtist(artistEntity: ArtistEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun overrideLinkageSongAndArtist(linkage: LinkageSongAndArtist)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun overrideLinkageAlbumAndArtist(linkage: LinkageAlbumAndArtist)

    @Delete
    protected abstract fun removeSong(songEntity: SongEntity)
    @Delete
    protected abstract fun removeAlbum(albumEntity: AlbumEntity)
    @Delete
    protected abstract fun removeArtist(artistEntity: ArtistEntity)
    @Delete
    protected abstract fun removeLinkageSongAndArtist(linkage: LinkageSongAndArtist)
    @Delete
    protected abstract fun removeLinkageLinkageAlbumAndArtist(linkage: LinkageAlbumAndArtist)
}