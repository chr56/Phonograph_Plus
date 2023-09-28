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
import kotlin.math.max

@Dao
abstract class RelationShipDao {

    @Transaction
    open fun register(songEntity: SongEntity, albumDao: AlbumDao, artistDao: ArtistDao) {
        overrideSong(songEntity)
        registerArtists(songEntity, artistDao)
        registerAlbum(songEntity, albumDao)
    }


    @Transaction
    open fun unregister(songEntity: SongEntity, albumDao: AlbumDao, artistDao: ArtistDao) {
        removeSong(songEntity)
        //todo
    }

    companion object {
        private const val TAG: String = "RelationShipDao"
    }

    private fun registerAlbum(songEntity: SongEntity, albumDao: AlbumDao) {
        val albumName = songEntity.albumName
        if (albumName != null) {
            val old = albumDao.id(songEntity.albumId)
            @Suppress("IfThenToElvis")
            val album =
                if (old != null) {
                    // update
                    old.copy(
                        albumArtistName = old.albumArtistName.ifEmpty { songEntity.albumArtistName ?: "" },
                        dateModified = max(old.dateModified, songEntity.dateModified),
                        year = max(old.year, songEntity.year),
                        songCount = old.songCount + 1,
                    )
                } else {
                    // new
                    AlbumEntity(
                        albumId = songEntity.albumId,
                        albumName = albumName,
                        artistId = 0,
                        albumArtistName = songEntity.albumArtistName ?: "",
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
    ) {
        registerArtist(songEntity, artistDao, songEntity.rawArtistName, LinkageSongAndArtist.ROLE_ARTIST)
        registerArtist(songEntity, artistDao, songEntity.composer, LinkageSongAndArtist.ROLE_COMPOSER)
        registerArtist(songEntity, artistDao, songEntity.albumArtistName, LinkageSongAndArtist.ROLE_ALBUM_ARTIST)
    }

    private fun registerArtist(
        songEntity: SongEntity,
        artistDao: ArtistDao,
        raw: String?,
        @ArtistRole role: Int,
    ) {
        if (raw != null) {
            val parsed = splitMultiTag(raw)
            if (parsed.isNotEmpty()) {
                for (name in parsed) {
                    val old = artistDao.named(name)
                    @Suppress("IfThenToElvis")
                    val artist = if (old != null) {
                        // update
                        old.copy(
                            songCount = old.songCount + 1
                        )
                    } else {
                        ArtistEntity(name.hashCode().toLong(), name, songCount = 1)
                    }
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
    protected abstract fun overrideSong(songEntity: SongEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun overrideAlbum(albumEntity: AlbumEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun overrideArtist(artistEntity: ArtistEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun overrideLinkageSongAndArtist(linkage: LinkageSongAndArtist)

    @Delete
    protected abstract fun removeSong(songEntity: SongEntity)
    @Delete
    protected abstract fun removeAlbum(albumEntity: AlbumEntity)
    @Delete
    protected abstract fun removeArtist(artistEntity: ArtistEntity)
    @Delete
    protected abstract fun removeLinkageSongAndArtist(linkage: LinkageSongAndArtist)
}