/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.Song
import player.phonograph.model.repo.sync.SyncExecutor
import player.phonograph.model.repo.sync.ProgressConnection
import player.phonograph.model.repo.sync.SyncResult
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.MediaStoreSongs
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.converter.EntityConverter
import player.phonograph.repo.room.dao.AlbumDao
import player.phonograph.repo.room.dao.ArtistDao
import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.LinkageAlbumAndArtist
import player.phonograph.repo.room.entity.LinkageSongAndArtist
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_ALBUM_ARTIST
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_ARTIST
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_COMPOSER
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.util.text.splitMultiTag
import androidx.room.withTransaction
import android.content.Context
import kotlin.math.max


class BasicSyncExecutor(private val musicDatabase: MusicDatabase) : SyncExecutor {

    private val mediaStoreSongDao = musicDatabase.MediaStoreSongDao()

    override suspend fun check(context: Context): Boolean {

        val songsMediastore = songsFromMediastore(context)
        val latestMediastore = songsMediastore.maxByOrNull { it.dateModified }

        val songsDatabase = songsFromDatabase()
        val latestDatabase = songsDatabase.maxByOrNull { it.dateModified }

        return if (songsMediastore.size != songsDatabase.size || latestDatabase == null || latestMediastore == null) {
            true
        } else {
            latestMediastore.dateModified >= latestDatabase.dateModified
        }
    }

    override suspend fun sync(
        context: Context,
        channel: ProgressConnection?,
    ): SyncResult {
        val songsMediastore = songsFromMediastore(context)
        val total = songsMediastore.size
        channel?.onProcessUpdate(0, total)
        musicDatabase.withTransaction {
            mediaStoreSongDao.deleteAll()
            mediaStoreSongDao.insert(songsMediastore.map(EntityConverter::fromSongModel))
        }
        channel?.onProcessUpdate(total, total)
        return SyncResult(success = true, modified = total)
    }

    private suspend fun songsFromDatabase(): List<MediastoreSongEntity> =
        mediaStoreSongDao.all(SortMode(SortRef.MODIFIED_DATE, true))

    private suspend fun songsFromMediastore(context: Context): List<Song> =
        MediaStoreSongs.all(context)
}

class RelationshipSyncExecutor(private val musicDatabase: MusicDatabase) : SyncExecutor {

    override suspend fun check(context: Context): Boolean {
        val mediaStoreSongDao = musicDatabase.MediaStoreSongDao()

        val songsMediastore = MediaStoreSongs.all(context)
        val latestMediastore = songsMediastore.maxByOrNull { it.dateModified }

        val songsDatabase = mediaStoreSongDao.all(SortMode(SortRef.MODIFIED_DATE, true))
        val latestDatabase = songsDatabase.maxByOrNull { it.dateModified }

        return if (songsMediastore.size != songsDatabase.size || latestDatabase == null || latestMediastore == null) {
            true
        } else {
            latestMediastore.dateModified >= latestDatabase.dateModified
        }
    }

    override suspend fun sync(
        context: Context,
        channel: ProgressConnection?,
    ): SyncResult {

        val songs = MediaStoreSongs.all(context)
        val total = songs.size

        channel?.onProcessUpdate(0, total)
        for ((i, song) in songs.withIndex()) {
            register(musicDatabase, song.let(EntityConverter::fromSongModel))
            channel?.onProcessUpdate(i + 1, total, song.title)
        }
        channel?.onProcessUpdate(total, total)

        return SyncResult(success = true)
    }

    suspend fun register(
        musicDatabase: MusicDatabase,
        songEntity: MediastoreSongEntity,
    ) {
        val songDao = musicDatabase.MediaStoreSongDao()
        val albumDao = musicDatabase.AlbumDao()
        val artistDao = musicDatabase.ArtistDao()
        val queryDao = musicDatabase.QueryDao()
        val relationshipArtistAlbumDao = musicDatabase.RelationshipArtistAlbumDao()
        val relationshipArtistSongDao = musicDatabase.RelationshipArtistSongDao()

        musicDatabase.withTransaction {
            songDao.insert(songEntity)

            val parsedSong = ParsedSong.parse(songEntity, albumDao, artistDao)

            val registeredArtists = registerArtists(parsedSong, artistDao)
            val registeredAlbum = registerAlbum(parsedSong, registeredArtists, albumDao)

            // bump album song count
            if (registeredAlbum != null) {
                albumDao.insert(registeredAlbum.copy(songCount = registeredAlbum.songCount + 1))
            }

            // update artists cross reference
            for ((role, artists) in registeredArtists) {
                for (artist in artists) {
                    relationshipArtistSongDao.override(
                        LinkageSongAndArtist(parsedSong.song.mediastorId, artist.artistId, role)
                    )
                    relationshipArtistAlbumDao.override(
                        LinkageAlbumAndArtist(parsedSong.song.albumId, artist.artistId)
                    )
                }
            }

            // update artist song count
            val allArtists = registeredArtists.flatMap { it.value }
            for (artist in allArtists) {
                // a same album for this song may already registered for this artist, we could not simplify add 1
                val albumCount = queryDao.queryArtistAlbumCount(artist.artistId) // query LinkageAlbumAndArtist size
                artistDao.insert(artist.copy(albumCount = albumCount, songCount = artist.songCount + 1))
            }
        }
    }


    suspend fun unregister(
        musicDatabase: MusicDatabase,
        songEntity: MediastoreSongEntity,
    ) {
        val songDao = musicDatabase.MediaStoreSongDao()
        musicDatabase.withTransaction {
            songDao.delete(songEntity)
        }
        //todo
    }

    /**
     * @return registered artists
     */
    private suspend fun registerArtists(
        parsedSong: ParsedSong,
        artistDao: ArtistDao,
    ): Map<Int, Collection<ArtistEntity>> {

        return parsedSong.catalogedArtistes.mapValues { (role, artists) ->
            artists.mapNotNull { (name, existedArtist) ->
                @Suppress("IfThenToElvis")
                if (existedArtist == null) {
                    // create new
                    ArtistEntity(name.hashCode().toLong(), name, albumCount = 1, songCount = 1).also {
                        artistDao.insert(it)
                    }
                } else {
                    // use existed
                    existedArtist
                }
            }
        }
    }

    private suspend fun registerAlbum(
        parsedSong: ParsedSong,
        registerArtists: Map<Int, Collection<ArtistEntity>>,
        albumDao: AlbumDao,
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
            albumDao.insert(albumEntity)
            albumEntity
        } else {
            null
        }
    }

    private class ParsedSong private constructor(
        val song: MediastoreSongEntity,
        val albumName: String?,
        val album: AlbumEntity?,
        val defaultArtists: Map<String, ArtistEntity?>,
        val albumArtists: Map<String, ArtistEntity?>,
        val composerArtists: Map<String, ArtistEntity?>,
    ) {

        val catalogedArtistes: Map<Int, Map<String, ArtistEntity?>> = mapOf(
            ROLE_ARTIST to defaultArtists,
            ROLE_COMPOSER to composerArtists,
            ROLE_ALBUM_ARTIST to albumArtists,
        )

        // val allArtists = catalogedArtistes.mapValues { catalog -> catalog.value.map { artists -> artists.value } }

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
                    splitMultiTag(rawRawArtistName).associateWith { name -> artistDao.named(name) }
                } else emptyMap()

                val rawComposer = songEntity.composer
                val composerArtists = if (rawComposer.isNotEmpty()) {
                    splitMultiTag(rawComposer).associateWith { name -> artistDao.named(name) }
                } else emptyMap()

                val rawAlbumArtistName = songEntity.albumArtist
                val albumArtists = if (rawAlbumArtistName.isNotEmpty()) {
                    splitMultiTag(rawAlbumArtistName).associateWith { name -> artistDao.named(name) }
                } else emptyMap()


                return ParsedSong(
                    songEntity, albumName, album, defaultArtists, albumArtists, composerArtists
                )
            }
        }
    }
}

private const val TAG = "DatabaseSync"
