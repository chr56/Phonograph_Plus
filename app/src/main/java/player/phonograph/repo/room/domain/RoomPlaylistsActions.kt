/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.App
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.Song
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.entity.PlaylistEntity
import player.phonograph.repo.room.entity.PlaylistSongEntity

object RoomPlaylistsActions {

    suspend fun create(
        database: MusicDatabase,
        name: String,
        songs: Collection<Song>,
    ): Boolean {
        val id = create(database, name)
        return if (id > 0) {
            EventHub.sendEvent(App.instance, EventHub.EVENT_PLAYLISTS_CHANGED)
            amendSongs(database, id, songs) == songs.size
        } else {
            false
        }
    }

    suspend fun create(
        database: MusicDatabase,
        name: String,
        dateAdded: Long = currentTimestamp(),
        dateModified: Long = currentTimestamp(),
    ): Long {
        val playlistManipulateDao = database.PlaylistManipulateDao()
        return playlistManipulateDao.insert(PlaylistEntity(name = name, dateAdded = dateAdded, dateModified = dateModified))
    }

    suspend fun rename(
        database: MusicDatabase,
        playlistId: Long,
        newName: String,
    ): Boolean {
        val playlistQueryDao = database.PlaylistQueryDao()
        val playlistManipulateDao = database.PlaylistManipulateDao()
        val result = playlistManipulateDao.rename(playlistQueryDao, playlistId, newName)
        if (result) {
            playlistManipulateDao.modifyDate(playlistQueryDao, playlistId, currentTimestamp())
            EventHub.sendEvent(App.instance, EventHub.EVENT_PLAYLISTS_CHANGED)
        }
        return result
    }

    suspend fun containsSong(
        database: MusicDatabase,
        playlistId: Long,
        song: Song,
    ): Boolean {
        val playlistSongQueryDao = database.PlaylistSongQueryDao()
        return playlistSongQueryDao.count(playlistId, song.id) > 0
    }

    suspend fun amendSongs(
        database: MusicDatabase,
        id: Long,
        songs: Collection<Song>,
    ): Int {
        val playlistSongQueryDao = database.PlaylistSongQueryDao()
        val playlistSongManipulateDao = database.PlaylistSongManipulateDao()
        val indexOffset = playlistSongQueryDao.maximumIndexOf(id) + 1

        val entities = songs.mapIndexed { num, song ->
            PlaylistSongEntity(mediastoreId = song.id, path = song.data, playlistId = id, position = num + indexOffset)
        }

        val lines = playlistSongManipulateDao.insert(entities).size // lines of success
        if (lines > 0) {
            database.PlaylistManipulateDao().modifyDate(database.PlaylistQueryDao(), id, currentTimestamp())
            EventHub.sendEvent(App.instance, EventHub.EVENT_PLAYLISTS_CHANGED)
        }
        return lines
    }

    suspend fun removeSong(
        database: MusicDatabase,
        playlistId: Long,
        songId: Long,
        position: Int,
    ): Boolean {
        val playlistSongQueryDao = database.PlaylistSongQueryDao()
        val playlistSongManipulateDao = database.PlaylistSongManipulateDao()
        val result = playlistSongManipulateDao.removeItem(playlistSongQueryDao, playlistId, songId, position)
        if (result) {
            database.PlaylistManipulateDao().modifyDate(database.PlaylistQueryDao(), playlistId, currentTimestamp())
        }
        return result
    }

    suspend fun swapSong(
        database: MusicDatabase,
        playlistId: Long,
        positionA: Int,
        positionB: Int,
    ): Boolean {
        val playlistSongQueryDao = database.PlaylistSongQueryDao()
        val playlistSongManipulateDao = database.PlaylistSongManipulateDao()
        val result = playlistSongManipulateDao.swap(playlistSongQueryDao, playlistId, positionA, positionB)
        if (result) {
            database.PlaylistManipulateDao().modifyDate(database.PlaylistQueryDao(), playlistId, currentTimestamp())
        }
        return result
    }

    suspend fun moveSong(
        database: MusicDatabase,
        playlistId: Long,
        from: Int,
        to: Int,
    ): Boolean {
        val playlistSongQueryDao = database.PlaylistSongQueryDao()
        val playlistSongManipulateDao = database.PlaylistSongManipulateDao()
        val result = playlistSongManipulateDao.move(playlistSongQueryDao, playlistId, from, to)
        if (result) {
            database.PlaylistManipulateDao().modifyDate(database.PlaylistQueryDao(), playlistId, currentTimestamp())
        }
        return result
    }

    suspend fun delete(
        database: MusicDatabase,
        playlistId: Long,
    ): Boolean {
        val playlistQueryDao = database.PlaylistQueryDao()
        val playlistManipulateDao = database.PlaylistManipulateDao()
        val playlistEntity = playlistQueryDao.id(playlistId)
        return if (playlistEntity != null) {
            playlistManipulateDao.delete(playlistEntity) == 1
        } else {
            false
        }.also {
            EventHub.sendEvent(App.instance, EventHub.EVENT_PLAYLISTS_CHANGED)
        }
    }

    suspend fun import(
        database: MusicDatabase,
        name: String,
        songs: Collection<Song>,
        dateAdded: Long,
        dateModified: Long,
    ): Boolean {
        val id = create(database, name, dateAdded = dateAdded, dateModified = dateModified)
        return if (id > 0) {
            EventHub.sendEvent(App.instance, EventHub.EVENT_PLAYLISTS_CHANGED)
            amendSongs(database, id, songs) == songs.size
        } else {
            false
        }
    }

    private fun currentTimestamp(): Long = System.currentTimeMillis() / 1000
}