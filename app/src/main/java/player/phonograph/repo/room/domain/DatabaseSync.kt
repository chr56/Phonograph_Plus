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
import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.LinkageAlbumAndArtist
import player.phonograph.repo.room.entity.LinkageSongAndArtist
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_ALBUM_ARTIST
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_ARTIST
import player.phonograph.repo.room.entity.LinkageSongAndArtist.Companion.ROLE_COMPOSER
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.util.text.AccumulatedSongRelationship
import player.phonograph.util.text.SongRelationship
import androidx.room.withTransaction
import android.content.Context
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

suspend fun defaultCheck(context: Context, musicDatabase: MusicDatabase): Boolean {
    val songsMediastore = MediaStoreSongs.all(context)
    val latestMediastore = songsMediastore.maxByOrNull { it.dateModified }

    val songsCountDatabase = musicDatabase.MediaStoreSongDao().total()
    val latestDatabase = musicDatabase.MediaStoreSongDao().latest()

    return if (songsMediastore.size != songsCountDatabase || latestDatabase == null || latestMediastore == null) {
        true
    } else {
        latestMediastore.dateModified >= latestDatabase.dateModified
    }
}

class BasicSyncExecutor(private val musicDatabase: MusicDatabase) : SyncExecutor {

    override suspend fun check(context: Context): Boolean = defaultCheck(context, musicDatabase)

    override suspend fun sync(
        context: Context,
        channel: ProgressConnection?,
    ): SyncResult {
        val songsMediastore = MediaStoreSongs.all(context)
        val total = songsMediastore.size
        channel?.onProcessUpdate(0, total)
        val songDao = musicDatabase.MediaStoreSongDao()
        musicDatabase.withTransaction {
            songDao.deleteAll()
            songDao.update(songsMediastore.map(EntityConverter::fromSongModel))
        }
        channel?.onProcessUpdate(total, total)
        return SyncResult(success = true, modified = total)
    }

}

class RelationshipSyncExecutor(private val musicDatabase: MusicDatabase) : SyncExecutor {

    override suspend fun check(context: Context): Boolean = defaultCheck(context, musicDatabase)

    override suspend fun sync(
        context: Context,
        channel: ProgressConnection?,
    ): SyncResult {
        val session = RelationshipSyncExecutorSession(context, musicDatabase, channel)
        return session.execute()
    }

}

class RelationshipSyncExecutorSession(
    private val context: Context,
    private val musicDatabase: MusicDatabase,
    private val channel: ProgressConnection?,
) {
    val songDao = musicDatabase.MediaStoreSongDao()
    val albumDao = musicDatabase.AlbumDao()
    val artistDao = musicDatabase.ArtistDao()
    val queryDao = musicDatabase.QueryDao()
    val relationshipArtistSongDao = musicDatabase.RelationshipArtistSongDao()
    val relationshipArtistAlbumDao = musicDatabase.RelationshipArtistAlbumDao()

    private fun onProcessUpdate(current: Int, total: Int, message: String? = null) {
        if (message != null) {
            channel?.onProcessUpdate(current, total, message)
        } else {
            channel?.onProcessUpdate(current, total)
        }
    }

    suspend fun execute(): SyncResult {
        // Stage I: Update or insert
        val modified = stageRefresh()
        // Stage II: Delete
        val removed = stageClean()
        return SyncResult(success = true, modified = modified, removed = removed)
    }

    /**
     * Insert new ones or update modified ones
     */
    suspend fun stageRefresh(): Int {
        val latestInDatabase = songDao.latest()
        val cutoff = latestInDatabase?.dateModified ?: 0

        val newOrUpdated = MediaStoreSongs.since(context, timestamp = cutoff, useModifiedDate = true)
        if (newOrUpdated.isNotEmpty()) doRefresh(newOrUpdated)

        return newOrUpdated.size
    }

    private suspend fun doRefresh(newOrUpdated: List<Song>) {

        onProcessUpdate(0, 1)
        val relationships = newOrUpdated.map(SongRelationship::solve)
        val affected = AccumulatedSongRelationship.reduce(relationships)

        var process = 0
        val total = 1 + 3 + affected.albums.size * 2 + affected.artists.size * 2 + relationships.size

        onProcessUpdate(1, total)

        // Songs
        val affectedSongs = affected.songs.map(EntityConverter::fromSongModel)

        // Artists
        val (affectedArtists, newArtistNames) =
            lookupExistedArtists(affected.artists, process, total)
        process += affected.artists.size
        val newArtists = newArtistNames.map { name ->
            ArtistEntity(artistId = name.hashCode().toLong(), artistName = name)
        }

        fun lookupRelatedArtistId(name: String): Long {
            val artists = affectedArtists.filter { it.artistName == name }
                .ifEmpty { newArtists.filter { it.artistName == name } }
            return artists.firstOrNull()?.artistId ?: 0
        }

        // Albums
        val (affectedAlbums, newAlbumNames) =
            lookupExistedAlbums(affected.albums, process, total)
        process += affected.albums.size
        val newAlbums = createNewAlbums(
            newAlbumNames, relationships, process, total, lookupArtistId = ::lookupRelatedArtistId
        )
        process += newAlbumNames.size
        val modifiedAlbums = modifyAlbums(
            affectedAlbums, relationships, process, total, lookupArtistId = ::lookupRelatedArtistId
        )
        process += affectedAlbums.size

        // LinkageAlbumAndArtist
        val (linkageSongAndArtists, linkageAlbumAndArtists) =
            createLinkages(relationships, process, total, lookupArtistId = ::lookupRelatedArtistId)
        process += relationships.size

        musicDatabase.withTransaction {
            // Step I: Songs registry
            process += 1
            songDao.update(affectedSongs)
            onProcessUpdate(process, total)

            // Step II: Artists registry
            process += 1
            artistDao.update(newArtists)
            onProcessUpdate(process, total)

            // Step III: Albums registry
            process += 1
            albumDao.update(newAlbums)
            albumDao.update(modifiedAlbums)
            onProcessUpdate(process, total)

            // Step IV: Cross reference registry
            relationshipArtistAlbumDao.override(linkageAlbumAndArtists)
            relationshipArtistSongDao.override(linkageSongAndArtists)

            // Step V: counter updating
            for (artist in newArtists + affectedArtists) {
                process += 1
                val artistId = artist.artistId
                artistDao.updateCounter(
                    artistId = artistId,
                    songCount = queryDao.queryArtistSongCount(artistId),
                    albumCount = queryDao.queryArtistAlbumCount(artistId),
                )
                onProcessUpdate(process, total)
            }
        }
        onProcessUpdate(total, total)
    }

    private fun lookupExistedArtists(
        artists: Set<String?>,
        process: Int,
        total: Int,
    ): Pair<List<ArtistEntity>, List<String>> {
        if (artistDao.count() == 0) {
            // Create for first time, all are new
            val empty = emptyList<ArtistEntity>()
            val all = artists.filterNotNull().toList()
            onProcessUpdate(process + artists.size, total)
            return empty to all
        } else {
            val affectedArtists = mutableListOf<ArtistEntity>()
            val newArtistNames = mutableListOf<String>()
            var subprocess = 0
            for (name in artists) {
                subprocess += 1
                if (name.isNullOrEmpty()) continue
                val searched = artistDao.named(name)
                if (searched != null) {
                    affectedArtists.add(searched)
                } else {
                    newArtistNames.add(name)
                }
                onProcessUpdate(process + subprocess, total)
            }
            return affectedArtists.toList() to newArtistNames.toList()
        }
    }

    private fun lookupExistedAlbums(
        albums: Map<Long, String?>,
        process: Int,
        total: Int,
    ): Pair<List<AlbumEntity>, Map<Long, String>> {
        if (albumDao.count() == 0) {
            // Create for first time, all are new
            val empty = emptyList<AlbumEntity>()
            val all = albums.mapValues { it.value.orEmpty() }
            onProcessUpdate(process + albums.size, total)
            return empty to all
        } else {
            val affectedAlbums = mutableListOf<AlbumEntity>()
            val newAlbumNames = mutableMapOf<Long, String>()
            var subprocess = 0
            for ((id, name) in albums) {
                subprocess += 1
                if (name.isNullOrEmpty()) continue
                val searched = albumDao.id(id)
                if (searched != null) {
                    affectedAlbums.add(searched)
                } else {
                    newAlbumNames.put(id, name)
                }
                onProcessUpdate(process + subprocess, total)
            }
            return affectedAlbums.toList() to newAlbumNames.toMap()
        }
    }



    private fun createNewAlbums(
        newAlbumNames: Map<Long, String>,
        relationships: List<SongRelationship>,
        process: Int,
        total: Int,
        lookupArtistId: (String) -> Long,
    ): List<AlbumEntity> {
        var subprocess = 0
        return newAlbumNames.map { (id, name) ->
            subprocess += 1
            val albumSongs =
                relationships.filter { it.albumId == id }
            onProcessUpdate(process + subprocess, total)
            createNewAlbum(albumSongs, id, name, lookupArtistId = lookupArtistId)
        }
    }

    private fun createNewAlbum(
        albumSongs: List<SongRelationship>,
        id: Long,
        name: String,
        lookupArtistId: (String) -> Long,
    ): AlbumEntity {
        val year = albumSongs.maxOf { it.song.year }
        val dateModified = albumSongs.maxOf { it.song.dateModified }
        val candidateArtistList =
            albumSongs.flatMap { it.albumArtists }.ifEmpty { albumSongs.flatMap { it.defaultArtists } }
        val candidateArtist = candidateArtistList.firstOrNull().orEmpty()
        val artistId = lookupArtistId(candidateArtist)
        return AlbumEntity(
            albumId = id,
            albumName = name,
            artistId = artistId,
            albumArtistName = candidateArtist,
            year = year,
            dateModified = dateModified,
            songCount = albumSongs.size
        )
    }

    private fun modifyAlbums(
        affectedAlbums: List<AlbumEntity>,
        relationships: List<SongRelationship>,
        process: Int,
        total: Int,
        lookupArtistId: (String) -> Long,
    ): List<AlbumEntity> {
        var subprocess = 0
        return affectedAlbums.map { album ->
            subprocess += 1
            val albumSongs =
                relationships.filter { it.albumId == album.albumId || (it.album != null && it.album == album.albumName) }
            modifyAlbum(album, albumSongs, lookupArtistId = lookupArtistId).also {
                onProcessUpdate(process + subprocess, total)
            }
        }
    }

    private fun modifyAlbum(
        album: AlbumEntity,
        newAlbumSongs: List<SongRelationship>,
        lookupArtistId: (String) -> Long,
    ): AlbumEntity {
        var year = album.year
        var dateModified = album.dateModified
        val updateAlbumArtist = album.albumArtistName.isEmpty() || album.artistId <= 0
        var albumArtistName: String? = null

        for (songRelationship in newAlbumSongs) {
            if (songRelationship.song.year > year) year = songRelationship.song.year
            if (songRelationship.song.dateModified > dateModified) dateModified = songRelationship.song.dateModified
            if (updateAlbumArtist || albumArtistName == null) {
                albumArtistName =
                    songRelationship.albumArtists.firstOrNull() ?: songRelationship.defaultArtists.firstOrNull()
            }
        }
        return if (albumArtistName != null) {
            album.copy(
                year = year,
                dateModified = dateModified,
                songCount = album.songCount + newAlbumSongs.size,
                albumArtistName = albumArtistName,
                artistId = lookupArtistId(albumArtistName),
            )
        } else {
            album.copy(
                year = year,
                dateModified = dateModified,
                songCount = album.songCount + newAlbumSongs.size
            )
        }
    }

    private fun createLinkages(
        relationships: List<SongRelationship>,
        process: Int,
        total: Int,
        lookupArtistId: (String) -> Long,
    ): Pair<List<LinkageSongAndArtist>, List<LinkageAlbumAndArtist>> {
        val linkageSongAndArtists = mutableListOf<LinkageSongAndArtist>()
        val linkageAlbumAndArtists = mutableListOf<LinkageAlbumAndArtist>()
        var subprocess = 0
        for (relationship in relationships) {
            subprocess += 1
            linkageAlbumAndArtists.addAll(
                relationship.artists.map { name ->
                    LinkageAlbumAndArtist(
                        albumId = relationship.albumId,
                        artistId = lookupArtistId(name)
                    )
                }
            )
            linkageSongAndArtists.addAll(
                relationship.albumArtists.map { name ->
                    LinkageSongAndArtist(
                        songId = relationship.song.id,
                        artistId = lookupArtistId(name),
                        role = ROLE_ALBUM_ARTIST,
                    )
                }
            )
            linkageSongAndArtists.addAll(
                relationship.defaultArtists.map { name ->
                    LinkageSongAndArtist(
                        songId = relationship.song.id,
                        artistId = lookupArtistId(name),
                        role = ROLE_ARTIST,
                    )
                }
            )
            linkageSongAndArtists.addAll(
                relationship.composerArtists.map { name ->
                    LinkageSongAndArtist(
                        songId = relationship.song.id,
                        artistId = lookupArtistId(name),
                        role = ROLE_COMPOSER,
                    )
                }
            )

            onProcessUpdate(process + subprocess, total)
        }
        return linkageSongAndArtists.toList() to linkageAlbumAndArtists.toList()
    }

    /**
     * Remove deleted ones
     */
    suspend fun stageClean(): Int {
        val includedSize = songDao.total()
        val allSize = MediaStoreSongs.all(context).size
        val deleted =
            if (allSize != includedSize) {
                doClean()
            } else {
                0
            }
        return deleted
    }

    private suspend fun doClean(): Int {

        var process = 0
        val all = songDao.total()
        var total = 1 + all

        onProcessUpdate(0, total)
        // Firstly search missing
        val allInDatabase = songDao.all(SortMode(SortRef.MODIFIED_DATE, true))
        process += 1
        val missing = searchMissing(context, allInDatabase, process, total, channel)
        process += all

        total += missing.size // update total

        // And relationships
        val allArtistRelationships: MutableList<LinkageSongAndArtist> = mutableListOf()
        val allAffectedArtists: MutableSet<Long> = mutableSetOf()
        val allAffectedAlbums: MutableSet<Long> = mutableSetOf()
        for (song in missing) {
            process += 1

            // Artist
            val artistRelationships = relationshipArtistSongDao.song(song.mediastorId)
            allArtistRelationships.addAll(artistRelationships)
            val affectedArtists = artistRelationships.map { it.artistId }.toSet()
            allAffectedArtists.addAll(affectedArtists)
            // Albums
            val album = albumDao.id(song.albumId)
            if (album != null) {
                allAffectedAlbums.add(album.albumId)
            }

            onProcessUpdate(process, total)
        }
        total += 1 + allAffectedArtists.size * 2 + allAffectedAlbums.size// update total

        // Then actual deletion
        musicDatabase.withTransaction {
            // Step I: Songs
            songDao.delete(missing)
            process += 1
            onProcessUpdate(process, total)

            // Step II: Artists relationship & song count
            relationshipArtistSongDao.remove(allArtistRelationships)
            for (artistId in allAffectedArtists) {
                process += 1
                val songCount = queryDao.queryArtistSongCount(artistId)
                if (songCount <= 0) {
                    artistDao.delete(artistId)
                    relationshipArtistAlbumDao.removeArtist(artistId)
                } else {
                    artistDao.updateCounter(artistId = artistId, songCount = songCount)
                }
                onProcessUpdate(process, total)
            }

            // Step III: Albums song count
            for (albumId in allAffectedAlbums) {
                process += 1
                val songCount = queryDao.queryAlbumSongCount(albumId)
                if (songCount <= 0) {
                    albumDao.delete(albumId)
                    relationshipArtistAlbumDao.removeAlbum(albumId)
                } else {
                    albumDao.updateCounter(albumId = albumId, songCount = songCount)
                }
                onProcessUpdate(process, total)
            }

            // Step IV: Artists album count
            for (artistId in allAffectedArtists) {
                process += 1
                val albumCount = queryDao.queryArtistAlbumCount(artistId)
                artistDao.updateCounter(artistId = artistId, albumCount = albumCount)
                onProcessUpdate(process, total)
            }
        }
        onProcessUpdate(total, total)

        return missing.size
    }

    private suspend fun searchMissing(
        context: Context,
        allInDatabase: List<MediastoreSongEntity>,
        process: Int,
        total: Int,
    ): List<MediastoreSongEntity> {
        var subprocess = 0
        val missing = mutableListOf<MediastoreSongEntity>()
        for (song in allInDatabase) {
            subprocess += 1
            if (MediaStoreSongs.id(context, song.mediastorId) == null) {
                missing.add(song)
            }
            onProcessUpdate(process + subprocess, total)
        }
        return missing.toList()
    }

}

private const val TAG = "DatabaseSync"
