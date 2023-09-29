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
import player.phonograph.repo.room.entity.SongEntity
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
    open fun register(songEntity: SongEntity, albumDao: AlbumDao, artistDao: ArtistDao) {
        overrideSong(songEntity)
        val parsedSong = ParsedSong.parse(songEntity, albumDao, artistDao) { queryLinkageAlbumAndArtist(it) }

        val registerArtists = registerArtists(parsedSong)
        registerAlbum(parsedSong, registerArtists)
    }


    @Transaction
    open fun unregister(songEntity: SongEntity, albumDao: AlbumDao, artistDao: ArtistDao) {
        removeSong(songEntity)
        //todo
    }

    private class ParsedSong private constructor(
        val song: SongEntity,
        val albumName: String?,
        val album: AlbumEntity?,
        val defaultArtists: Map<String, ArtistEntity?>,
        val albumArtists: Map<String, ArtistEntity?>,
        val composerArtists: Map<String, ArtistEntity?>,
        val knownAlbums: Map<ArtistEntity?, List<Long>>,
    ) {
        companion object {

            fun parse(
                songEntity: SongEntity,
                albumDao: AlbumDao,
                artistDao: ArtistDao,
                queryAlbum: (artistId: Long) -> Collection<LinkageAlbumAndArtist>,
            ): ParsedSong {
                val albumName = songEntity.albumName
                val album = if (albumName != null) albumDao.named(albumName) else null

                val rawRawArtistName = songEntity.rawArtistName
                val defaultArtists = if (rawRawArtistName != null) {
                    val tags = splitMultiTag(rawRawArtistName)
                    tags.associateWith { name ->
                        artistDao.named(name)
                    }
                } else emptyMap()

                val rawComposer = songEntity.composer
                val albumArtists = if (rawComposer != null) {
                    val tags = splitMultiTag(rawComposer)
                    tags.associateWith { name ->
                        artistDao.named(name)
                    }
                } else emptyMap()

                val rawAlbumArtistName = songEntity.albumArtistName
                val composerArtists = if (rawAlbumArtistName != null) {
                    val tags = splitMultiTag(rawAlbumArtistName)
                    tags.associateWith { name ->
                        artistDao.named(name)
                    }
                } else emptyMap()


                fun queryAlbums(map: Map<String, ArtistEntity?>): Map<ArtistEntity?, List<Long>> =
                    map
                        .mapKeys { it.value }
                        .mapValues { (_, artist) ->
                            val artistId = artist?.artistId ?: -1
                            if (artistId > 0)
                                queryAlbum(artistId).map { it.albumId }
                            else
                                emptyList()
                        }

                val r = queryAlbums(defaultArtists)
                val a = queryAlbums(albumArtists)
                val c = queryAlbums(composerArtists)

                val knownAlbums = r + a + c

                return ParsedSong(
                    songEntity, albumName, album, defaultArtists, albumArtists, composerArtists, knownAlbums
                )
            }

        }
    }

    companion object {
        private const val TAG: String = "RelationShipDao"
    }

    private fun registerAlbum(parsedSong: ParsedSong, registerArtists: Map<Int, Collection<ArtistEntity>>) {
        val albumName = parsedSong.albumName
        val existedAlbum = parsedSong.album
        val targetArtist: ArtistEntity? = run {
            registerArtists[ROLE_ALBUM_ARTIST]?.firstOrNull() ?: registerArtists[ROLE_ARTIST]?.firstOrNull()
        }
        if (albumName != null) {
            @Suppress("IfThenToElvis")
            val albumEntity = if (existedAlbum != null) {
                // update
                existedAlbum.copy(
                    artistId = targetArtist?.artistId ?: 0,
                    albumArtistName = targetArtist?.artistName ?: "",
                    dateModified = max(existedAlbum.dateModified, parsedSong.song.dateModified),
                    year = max(existedAlbum.year, parsedSong.song.year),
                    songCount = existedAlbum.songCount + 1,
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
        }
    }

    /**
     * @return registered artists
     */
    private fun registerArtists(parsedSong: ParsedSong): Map<Int, Collection<ArtistEntity>> {
        val r =
            registerArtist(parsedSong.song, ROLE_ARTIST, parsedSong.defaultArtists, parsedSong.knownAlbums)
        val c =
            registerArtist(parsedSong.song, ROLE_COMPOSER, parsedSong.composerArtists, parsedSong.knownAlbums)
        val a =
            registerArtist(parsedSong.song, ROLE_ALBUM_ARTIST, parsedSong.albumArtists, parsedSong.knownAlbums)
        return mapOf(ROLE_ARTIST to r, ROLE_COMPOSER to c, ROLE_ALBUM_ARTIST to a)
    }

    /**
     * @return registered artists
     */
    private fun registerArtist(
        songEntity: SongEntity,
        @ArtistRole role: Int,
        artists: Map<String, ArtistEntity?>,
        knownAlbums: Map<ArtistEntity?, List<Long>>,
    ): Collection<ArtistEntity> {
        return artists.map { (name, existedArtist) ->
            val artist = if (existedArtist != null) {
                // update existed
                val albumList = knownAlbums[existedArtist]
                val bumpAlbumCount = !(albumList != null && songEntity.albumId in albumList)
                val albumCount = if (bumpAlbumCount) existedArtist.albumCount + 1 else existedArtist.albumCount
                existedArtist.copy(
                    albumCount = albumCount,
                    songCount = existedArtist.songCount + 1
                )
            } else {
                // create new
                ArtistEntity(name.hashCode().toLong(), name, albumCount = 1, songCount = 1)
            }
            overrideArtist(artist)
            overrideLinkageSongAndArtist(LinkageSongAndArtist(songEntity.id, artist.artistId, role))
            overrideLinkageAlbumAndArtist(LinkageAlbumAndArtist(songEntity.albumId, artist.artistId))
            debug {
                Log.v(TAG, "* Registering Artist: ${songEntity.title} <--> ${artist.artistName} [$role]")
            }
            artist
        }
    }

    @Query("SELECT * from ${Tables.LINKAGE_ARTIST_ALBUM} where ${Columns.ARTIST_ID} = :artistId")
    protected abstract fun queryLinkageAlbumAndArtist(artistId: Long): List<LinkageAlbumAndArtist>

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