/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.Song
import player.phonograph.model.repo.sync.ProgressConnection
import player.phonograph.model.repo.sync.SyncExecutor
import player.phonograph.model.repo.sync.SyncReport
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.MediaStoreGenres
import player.phonograph.repo.mediastore.MediaStoreSongs
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.converter.EntityConverter
import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.GenreEntity
import player.phonograph.repo.room.entity.LinkageAlbumAndArtist
import player.phonograph.repo.room.entity.LinkageGenreAndSong
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
    ): SyncReport {
        val songsMediastore = MediaStoreSongs.all(context)
        val total = songsMediastore.size
        channel?.onProcessUpdate(0, total)
        val songDao = musicDatabase.MediaStoreSongDao()
        musicDatabase.withTransaction {
            songDao.deleteAll()
            songDao.update(songsMediastore.map(EntityConverter::fromSongModel))
        }
        channel?.onProcessUpdate(total, total)
        return SyncReport(success = true, modified = total)
    }

}

class RelationshipSyncExecutor(private val musicDatabase: MusicDatabase) : SyncExecutor {

    override suspend fun check(context: Context): Boolean = defaultCheck(context, musicDatabase)

    override suspend fun sync(
        context: Context,
        channel: ProgressConnection?,
    ): SyncReport {
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
    val genreDao = musicDatabase.GenreDao()
    val queryDao = musicDatabase.QueryDao()
    val relationshipArtistSongDao = musicDatabase.RelationshipArtistSongDao()
    val relationshipArtistAlbumDao = musicDatabase.RelationshipArtistAlbumDao()
    val relationshipGenreSongDao = musicDatabase.RelationshipGenreSongDao()

    private fun onProcessUpdate(current: Int, total: Int, message: String? = null) {
        if (message != null) {
            channel?.onProcessUpdate(current, total, message)
        } else {
            channel?.onProcessUpdate(current, total)
        }
    }

    suspend fun execute(): SyncReport {
        // Stage I: Update or insert
        val modified = stageRefresh()
        // Stage II: Delete
        val removed = stageClean()

        return SyncReport(success = true, modified = modified, removed = removed)
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

        onProcessUpdate(0, 1, "Reducing relationships")
        val relationships = newOrUpdated.map(SongRelationship::solve)
        val affected = AccumulatedSongRelationship.reduce(relationships)

        var process = 1
        val total =
            1 + 5 + affected.albums.size * 2 + affected.artists.size * 2 + relationships.size + newOrUpdated.size

        onProcessUpdate(process, total, "Calculate relationships")

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

        // Genres
        onProcessUpdate(process, total, "Analyzing Genres")
        val genreMediastoreMap =
            genreDao.all(SortMode(SortRef.MODIFIED_DATE, true))
                .associateBy { it.mediastoreId }
                .toMutableMap() // Map: MediaStoreGenreID -> GenreEntity.
        val newGenresToInsert = mutableListOf<GenreEntity>()
        val songToGenreMap = mutableMapOf<Long, List<Long>>()
        analyzeGenres(newOrUpdated, songToGenreMap, genreMediastoreMap, newGenresToInsert, process, total)
        process += newOrUpdated.size

        musicDatabase.withTransaction {
            // Step I: Songs registry
            onProcessUpdate(process, total, "Write all affected songs")
            songDao.update(affectedSongs)
            process += 1

            // Step II: Artists registry
            onProcessUpdate(process, total, "Write all affected artists")
            artistDao.update(newArtists)
            process += 1

            // Step III: Albums registry
            onProcessUpdate(process, total, "Write all affected albums")
            albumDao.update(newAlbums)
            albumDao.update(modifiedAlbums)
            process += 1

            // Step IV: Cross reference registry
            onProcessUpdate(process, total, "Write artist-albums relationships")
            relationshipArtistAlbumDao.override(linkageAlbumAndArtists)
            process += 1

            onProcessUpdate(process, total, "Write artist-songs relationships")
            relationshipArtistSongDao.override(linkageSongAndArtists)
            process += 1

            // Step V: counter updating

            onProcessUpdate(process, total, "Update artist songs/album counters")
            for (artist in newArtists + affectedArtists) {
                process += 1
                val artistId = artist.artistId
                artistDao.updateCounter(
                    artistId = artistId,
                    songCount = queryDao.queryArtistSongCount(artistId),
                    albumCount = queryDao.queryArtistAlbumCount(artistId),
                )
                if (process % PBI == 0)
                    onProcessUpdate(process, total, "Update artist songs/album counters")
            }

            // Step VI: Genres update
            onProcessUpdate(process, total, "Update all affected genres")
            for (newGenre in newGenresToInsert) {
                // insert new Genres and update genre map with concrete id
                val newId = genreDao.update(newGenre)
                genreMediastoreMap[newGenre.mediastoreId] = newGenre.copy(id = newId)
            }

            // Step VII: Genres song relationships
            onProcessUpdate(process, total, "Analyzing genre-songs relationship")
            val affectedGenreIds = mutableSetOf<Long>()
            val newGenreLinkages = mutableListOf<LinkageGenreAndSong>()
            for (song in newOrUpdated) {
                // Remove old linkages for this song
                relationshipGenreSongDao.removeSong(song.id)
                // Build new linkages
                val genreMediastoreIds = songToGenreMap[song.id] ?: emptyList()
                for (mediastoreId in genreMediastoreIds) {
                    val genreEntity = genreMediastoreMap[mediastoreId]
                    if (genreEntity != null) {
                        newGenreLinkages.add(LinkageGenreAndSong(genreEntity.id, song.id))
                        affectedGenreIds.add(genreEntity.id) // for updating counter
                    }
                }
            }
            onProcessUpdate(process, total, "Write genre-songs relationships")
            relationshipGenreSongDao.override(newGenreLinkages)

            // Step VII: Genres song counter
            onProcessUpdate(process, total, "Update genre-songs counter")
            for (genreId in affectedGenreIds) {
                val count = relationshipGenreSongDao.songIds(genreId).size
                genreDao.updateCounter(genreId, count)
            }
        }
        onProcessUpdate(total, total, "All done")
    }

    private fun lookupExistedArtists(
        artists: Set<String?>,
        process: Int,
        total: Int,
    ): Pair<List<ArtistEntity>, List<String>> {
        onProcessUpdate(process, total, "Compare with existed artists")
        if (artistDao.count() == 0) {
            // Create for first time, all are new
            val empty = emptyList<ArtistEntity>()
            val all = artists.filterNotNull().toList()
            onProcessUpdate(process + artists.size, total, "No artists")
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
                if (subprocess % PBI == 0)
                    onProcessUpdate(process + subprocess, total, "Compare with existed artists")
            }
            return affectedArtists.toList() to newArtistNames.toList()
        }
    }

    private fun lookupExistedAlbums(
        albums: Map<Long, String?>,
        process: Int,
        total: Int,
    ): Pair<List<AlbumEntity>, Map<Long, String>> {
        onProcessUpdate(process, total, "Compare with existed albums")
        if (albumDao.count() == 0) {
            // Create for first time, all are new
            val empty = emptyList<AlbumEntity>()
            val all = albums.mapValues { it.value.orEmpty() }
            onProcessUpdate(process + albums.size, total, "No albums")
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
                if (subprocess % PBI == 0)
                    onProcessUpdate(process + subprocess, total, "Compare with existed albums")
            }
            return affectedAlbums.toList() to newAlbumNames.toMap()
        }
    }

    private suspend fun analyzeGenres(
        newOrUpdated: List<Song>,
        songToGenreMap: MutableMap<Long, List<Long>>,
        genreMediastoreMap: MutableMap<Long, GenreEntity>,
        newGenresToInsert: MutableList<GenreEntity>,
        process: Int,
        total: Int,
    ) {
        val seenNewGenreIds = mutableSetOf<Long>()
        for ((index, song) in newOrUpdated.withIndex()) {
            val genres = MediaStoreGenres.of(context, song.id)
            val genreIds = genres.map { it.id }
            songToGenreMap[song.id] = genreIds
            for (genre in genres) {
                if (!genreMediastoreMap.containsKey(genre.id) && !seenNewGenreIds.contains(genre.id)) {
                    val entity = EntityConverter.fromGenreModel(genre)
                    newGenresToInsert.add(entity)
                    seenNewGenreIds.add(genre.id)
                }
            }
            if (index % PBI == 0) onProcessUpdate(process + index, total, "Analyzing Genres")
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
        onProcessUpdate(process, total, "Create new albums")
        return newAlbumNames.map { (id, name) ->
            subprocess += 1
            val albumSongs =
                relationships.filter { it.albumId == id }
            if (subprocess % PBI == 0) onProcessUpdate(process + subprocess, total, "Create new albums")
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
        onProcessUpdate(process, total, "Refresh existed albums")
        return affectedAlbums.map { album ->
            subprocess += 1
            val albumSongs =
                relationships.filter { it.albumId == album.albumId || (it.album != null && it.album == album.albumName) }
            modifyAlbum(album, albumSongs, lookupArtistId = lookupArtistId).also {
                if (subprocess % PBI == 0)
                    onProcessUpdate(process + subprocess, total, "Refresh existed albums")
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
        onProcessUpdate(process, total, "Create relationships")
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


            if (subprocess % PBI == 0)
                onProcessUpdate(process + subprocess, total, "Create relationships")
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

        onProcessUpdate(0, total, "Find deleted songs")
        // Firstly search missing
        val allInDatabase = songDao.all(SortMode(SortRef.MODIFIED_DATE, true))
        process += 1
        val missing = searchMissing(context, allInDatabase, process, total)
        process += all

        total += missing.size // update total

        onProcessUpdate(process, total, "Check relationships")
        // And relationships
        val allArtistRelationships: MutableList<LinkageSongAndArtist> = mutableListOf()
        val allAffectedArtists: MutableSet<Long> = mutableSetOf()
        val allAffectedAlbums: MutableSet<Long> = mutableSetOf()

        val allGenreRelationships: MutableList<LinkageGenreAndSong> = mutableListOf()
        val allAffectedGenres: MutableSet<Long> = mutableSetOf()

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

            // genres
            val genreRelationships = relationshipGenreSongDao.song(song.mediastorId)
            allGenreRelationships.addAll(genreRelationships)
            allAffectedGenres.addAll(genreRelationships.map { it.genreId })

            if (process % PBI == 0) onProcessUpdate(process, total, "Check relationships")
        }

        total += 1 + allAffectedArtists.size * 2 + allAffectedAlbums.size + allAffectedGenres.size // update total

        // Then actual deletion
        musicDatabase.withTransaction {
            // Step I: Songs
            onProcessUpdate(process, total, "Remove deleted songs")
            songDao.delete(missing)
            process += 1

            // Step II: Artists relationship & song count
            relationshipArtistSongDao.remove(allArtistRelationships)
            for (artistId in allAffectedArtists) {
                if (process % PBI == 0) onProcessUpdate(process, total, "Remove or update artists")
                process += 1
                val songCount = queryDao.queryArtistSongCount(artistId)
                if (songCount <= 0) {
                    artistDao.delete(artistId)
                    relationshipArtistAlbumDao.removeArtist(artistId)
                } else {
                    artistDao.updateCounter(artistId = artistId, songCount = songCount)
                }
            }

            // Step III: Albums song count
            for (albumId in allAffectedAlbums) {
                if (process % PBI == 0) onProcessUpdate(process, total, "Remove or update albums")
                process += 1
                val songCount = queryDao.queryAlbumSongCount(albumId)
                if (songCount <= 0) {
                    albumDao.delete(albumId)
                    relationshipArtistAlbumDao.removeAlbum(albumId)
                } else {
                    albumDao.updateCounter(albumId = albumId, songCount = songCount)
                }
            }

            // Step IV: Artists album count
            for (artistId in allAffectedArtists) {
                if (process % PBI == 0) onProcessUpdate(process, total, "Recounting artist albums")
                process += 1
                val albumCount = queryDao.queryArtistAlbumCount(artistId)
                artistDao.updateCounter(artistId = artistId, albumCount = albumCount)
            }

            // Step V: Genres & their relationships
            relationshipGenreSongDao.remove(allGenreRelationships)
            for (genreId in allAffectedGenres) {
                if (process % PBI == 0) onProcessUpdate(process, total, "Remove or update genres")
                process += 1

                // Recount or delete if empty
                val count = relationshipGenreSongDao.songIds(genreId).size
                if (count <= 0) {
                    genreDao.delete(genreId)
                } else {
                    genreDao.updateCounter(genreId, count)
                }
            }
        }
        onProcessUpdate(total, total, "All done")

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
            onProcessUpdate(process + subprocess, total, "Find deleted songs")
        }
        return missing.toList()
    }

}

private const val PBI = 32 // Progress bump interval

private const val TAG = "DatabaseSync"
