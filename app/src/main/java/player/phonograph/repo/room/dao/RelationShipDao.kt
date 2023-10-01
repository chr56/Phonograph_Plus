/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.LinkageAlbumAndArtist
import player.phonograph.repo.room.entity.LinkageSongAndArtist
import player.phonograph.repo.room.entity.LinkageSongAndArtist.ArtistRole
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_ALBUM_ARTIST
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_ARTIST
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_COMPOSER
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.Tables
import player.phonograph.util.debug
import player.phonograph.util.text.splitMultiTag
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import android.util.Log
import kotlin.math.max

@Dao
abstract class RelationShipDao {

    @Transaction
    open fun register(songEntity: MediastoreSongEntity, albumDao: AlbumDao, artistDao: ArtistDao) {
        overrideSong(songEntity)
        val parsedSong = ParsedSong.parse(songEntity, albumDao, artistDao)

        val registeredArtists = registerArtists(parsedSong)
        val registeredAlbum = registerAlbum(parsedSong, registeredArtists)

        for (artistEntity in registeredArtists.flatMap { it.value }) {
            updateArtistCounter(artistEntity)
        }
        if (registeredAlbum != null) {
            updateAlbumCounter(registeredAlbum)
        }
    }


    @Transaction
    open fun unregister(songEntity: MediastoreSongEntity, albumDao: AlbumDao, artistDao: ArtistDao) {
        removeSong(songEntity)
        //todo
    }

    private class ParsedSong private constructor(
        val song: MediastoreSongEntity,
        val albumName: String?,
        val album: AlbumEntity?,
        val defaultArtists: Map<String, ArtistEntity?>,
        val albumArtists: Map<String, ArtistEntity?>,
        val composerArtists: Map<String, ArtistEntity?>,
    ) {
        companion object {

            fun parse(
                songEntity: MediastoreSongEntity,
                albumDao: AlbumDao,
                artistDao: ArtistDao,
            ): ParsedSong {
                val albumName = songEntity.album
                val album = if (albumName.isNotEmpty()) albumDao.named(albumName) else null

                val rawRawArtistName = songEntity.artist
                val defaultArtists = if (rawRawArtistName.isNotEmpty()) {
                    val tags = splitMultiTag(rawRawArtistName)
                    tags.associateWith { name ->
                        artistDao.named(name)
                    }
                } else emptyMap()

                val rawComposer = songEntity.composer
                val albumArtists = if (rawComposer.isNotEmpty()) {
                    val tags = splitMultiTag(rawComposer)
                    tags.associateWith { name ->
                        artistDao.named(name)
                    }
                } else emptyMap()

                val rawAlbumArtistName = songEntity.albumArtist
                val composerArtists = if (rawAlbumArtistName.isNotEmpty()) {
                    val tags = splitMultiTag(rawAlbumArtistName)
                    tags.associateWith { name ->
                        artistDao.named(name)
                    }
                } else emptyMap()


                return ParsedSong(
                    songEntity, albumName, album, defaultArtists, albumArtists, composerArtists
                )
            }

        }
    }

    companion object {
        private const val TAG: String = "RelationShipDao"
    }

    private fun registerAlbum(
        parsedSong: ParsedSong,
        registerArtists: Map<Int, Collection<ArtistEntity>>,
    ): AlbumEntity? {
        val albumName = parsedSong.albumName
        val existedAlbum = parsedSong.album
        val targetArtist: ArtistEntity? = run {
            registerArtists[ROLE_ALBUM_ARTIST]?.firstOrNull() ?: registerArtists[ROLE_ARTIST]?.firstOrNull()
        }
        return if (albumName != null) {
            @Suppress("IfThenToElvis")
            val albumEntity = if (existedAlbum != null) {
                // update
                existedAlbum.copy(
                    artistId = targetArtist?.artistId ?: 0,
                    albumArtistName = targetArtist?.artistName ?: "",
                    dateModified = max(existedAlbum.dateModified, parsedSong.song.dateModified),
                    year = max(existedAlbum.year, parsedSong.song.year),
                )
            } else {
                // new
                AlbumEntity(
                    albumId = parsedSong.song.albumId,
                    albumName = albumName,
                    artistId = targetArtist?.artistId ?: 0,
                    albumArtistName = targetArtist?.artistName ?: "",
                    year = parsedSong.song.year,
                    dateModified = parsedSong.song.dateModified,
                    songCount = 1,
                )
            }
            overrideAlbum(albumEntity)
            albumEntity
        } else {
            null
        }
    }

    /**
     * @return registered artists
     */
    private fun registerArtists(parsedSong: ParsedSong): Map<Int, Collection<ArtistEntity>> {
        val r =
            registerArtist(parsedSong.song, ROLE_ARTIST, parsedSong.defaultArtists)
        val c =
            registerArtist(parsedSong.song, ROLE_COMPOSER, parsedSong.composerArtists)
        val a =
            registerArtist(parsedSong.song, ROLE_ALBUM_ARTIST, parsedSong.albumArtists)
        return mapOf(ROLE_ARTIST to r, ROLE_COMPOSER to c, ROLE_ALBUM_ARTIST to a)
    }

    /**
     * @return registered artists
     */
    private fun registerArtist(
        songEntity: MediastoreSongEntity,
        @ArtistRole role: Int,
        artists: Map<String, ArtistEntity?>,
    ): Collection<ArtistEntity> {
        return artists.map { (name, existedArtist) ->
            @Suppress("IfThenToElvis")
            val artist = if (existedArtist == null) {
                // create new
                ArtistEntity(name.hashCode().toLong(), name, albumCount = 1, songCount = 1).also { overrideArtist(it) }
            } else {
                // update existed
                existedArtist
            }
            overrideLinkageSongAndArtist(LinkageSongAndArtist(songEntity.mediastorId, artist.artistId, role))
            overrideLinkageAlbumAndArtist(LinkageAlbumAndArtist(songEntity.albumId, artist.artistId))
            debug {
                Log.v(TAG, "* Registering Artist: ${songEntity.title} <--> ${artist.artistName} [$role]")
            }
            artist
        }
    }


    private fun updateArtistCounter(artistEntity: ArtistEntity) {
        val songCount = queryArtistSongCount(artistEntity.artistId)
        val albumCount = queryArtistAlbumCount(artistEntity.artistId)
        overrideArtist(artistEntity.copy(albumCount = albumCount, songCount = songCount))
    }

    private fun updateAlbumCounter(albumEntity: AlbumEntity) {
        val count = queryAlbumSongCount(albumEntity.albumId)
        overrideAlbum(albumEntity.copy(songCount = count))
    }

    @Query("SELECT COUNT(*) from ${Tables.MEDIASTORE_SONGS} where ${Columns.ALBUM_ID} = :albumId")
    abstract fun queryAlbumSongCount(albumId: Long): Int

    @Query("SELECT COUNT(${Columns.ALBUM_ID}) from ${Tables.LINKAGE_ARTIST_ALBUM} where ${Columns.ARTIST_ID} = :artistId")
    protected abstract fun queryArtistAlbumCount(artistId: Long): Int
    @Query("SELECT COUNT(${Columns.MEDIASTORE_ID}) from ${Tables.LINKAGE_ARTIST_SONG} where ${Columns.ARTIST_ID} = :artistId")
    protected abstract fun queryArtistSongCount(artistId: Long): Int

    @Query("SELECT * from ${Tables.LINKAGE_ARTIST_ALBUM} where ${Columns.ARTIST_ID} = :artistId")
    protected abstract fun queryLinkageAlbumAndArtist(artistId: Long): List<LinkageAlbumAndArtist>
    @Query("SELECT * from ${Tables.LINKAGE_ARTIST_SONG} where ${Columns.ARTIST_ID} = :artistId")
    protected abstract fun queryLinkageSongAndArtist(artistId: Long): List<LinkageSongAndArtist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun overrideSong(songEntity: MediastoreSongEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun overrideAlbum(albumEntity: AlbumEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun overrideArtist(artistEntity: ArtistEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun overrideLinkageSongAndArtist(linkage: LinkageSongAndArtist)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun overrideLinkageAlbumAndArtist(linkage: LinkageAlbumAndArtist)

    @Delete
    protected abstract fun removeSong(songEntity: MediastoreSongEntity)
    @Delete
    protected abstract fun removeAlbum(albumEntity: AlbumEntity)
    @Delete
    protected abstract fun removeArtist(artistEntity: ArtistEntity)
    @Delete
    protected abstract fun removeLinkageSongAndArtist(linkage: LinkageSongAndArtist)
    @Delete
    protected abstract fun removeLinkageLinkageAlbumAndArtist(linkage: LinkageAlbumAndArtist)
}