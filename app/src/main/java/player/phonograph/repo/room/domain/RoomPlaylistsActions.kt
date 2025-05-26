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

    fun create(
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

    fun create(
        database: MusicDatabase,
        name: String,
        dateAdded: Long = currentTimestamp(),
        dateModified: Long = currentTimestamp(),
    ): Long {
        val playlistDao = database.PlaylistDao()
        return playlistDao.insert(PlaylistEntity(name = name, dateAdded = dateAdded, dateModified = dateModified))
    }

    fun rename(
        database: MusicDatabase,
        playlistId: Long,
        newName: String,
    ): Boolean {
        val playlistDao = database.PlaylistDao()
        val result = playlistDao.rename(playlistId, newName)
        if (result) {
            playlistDao.modifyDate(playlistId, currentTimestamp())
            EventHub.sendEvent(App.instance, EventHub.EVENT_PLAYLISTS_CHANGED)
        }
        return result
    }

    fun containsSong(
        database: MusicDatabase,
        playlistId: Long,
        song: Song,
    ): Boolean {
        val playlistSongDao = database.PlaylistSongDao()
        return playlistSongDao.count(playlistId, song.id) > 0
    }

    fun amendSongs(
        database: MusicDatabase,
        id: Long,
        songs: Collection<Song>,
    ): Int {
        val playlistSongDao = database.PlaylistSongDao()
        val indexOffset = playlistSongDao.maximumIndexOf(id) + 1

        val entities = songs.mapIndexed { num, song ->
            PlaylistSongEntity(mediastoreId = song.id, path = song.data, playlistId = id, position = num + indexOffset)
        }

        val lines = playlistSongDao.insert(entities).size // lines of success
        if (lines > 0) {
            database.PlaylistDao().modifyDate(id, currentTimestamp())
            EventHub.sendEvent(App.instance, EventHub.EVENT_PLAYLISTS_CHANGED)
        }
        return lines
    }

    fun removeSong(
        database: MusicDatabase,
        playlistId: Long,
        songId: Long,
        position: Int,
    ): Boolean {
        val playlistSongDao = database.PlaylistSongDao()
        val result = playlistSongDao.removeItem(playlistId, songId, position)
        if (result) {
            database.PlaylistDao().modifyDate(playlistId, currentTimestamp())
        }
        return result
    }

    fun swapSong(
        database: MusicDatabase,
        playlistId: Long,
        positionA: Int,
        positionB: Int,
    ): Boolean {
        val playlistSongDao = database.PlaylistSongDao()
        val result = playlistSongDao.swap(playlistId, positionA, positionB)
        if (result) {
            database.PlaylistDao().modifyDate(playlistId, currentTimestamp())
        }
        return result
    }

    fun moveSong(
        database: MusicDatabase,
        playlistId: Long,
        from: Int,
        to: Int,
    ): Boolean {
        val playlistSongDao = database.PlaylistSongDao()
        val result = playlistSongDao.move(playlistId, from, to)
        if (result) {
            database.PlaylistDao().modifyDate(playlistId, currentTimestamp())
        }
        return result
    }

    fun delete(
        database: MusicDatabase,
        playlistId: Long,
    ): Boolean {
        val playlistDao = database.PlaylistDao()
        val playlistEntity = playlistDao.id(playlistId)
        return if (playlistEntity != null) {
            playlistDao.delete(playlistEntity) == 1
        } else {
            false
        }.also {
            EventHub.sendEvent(App.instance, EventHub.EVENT_PLAYLISTS_CHANGED)
        }
    }

    fun import(
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