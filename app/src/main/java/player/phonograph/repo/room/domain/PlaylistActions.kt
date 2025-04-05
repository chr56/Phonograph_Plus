/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.mechanism.broadcast.sentPlaylistChangedLocalBoardCast
import player.phonograph.model.Song
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.entity.PlaylistEntity
import player.phonograph.repo.room.entity.PlaylistSongEntity

@Suppress("MemberVisibilityCanBePrivate")
object PlaylistActions {

    fun createPlaylist(
        database: MusicDatabase,
        name: String,
        songs: Collection<Song>,
    ): Boolean {
        val id = createPlaylist(database, name)
        return if (id > 0) {
            sentPlaylistChangedLocalBoardCast()
            amendPlaylist(database, id, songs) == songs.size
        } else {
            false
        }
    }

    fun containsSongFromPlaylist(
        database: MusicDatabase,
        playlistId: Long,
        song: Song,
    ): Boolean {
        val playlistSongDao = database.PlaylistSongDao()
        return playlistSongDao.count(playlistId, song.id) > 0
    }


    fun createPlaylist(
        database: MusicDatabase,
        name: String,
    ): Long {
        val playlistDao = database.PlaylistDao()
        val timestamp = currentTimestamp()
        return playlistDao.insert(PlaylistEntity(name = name, dateAdded = timestamp, dateModified = timestamp))
    }

    fun renamePlaylist(
        database: MusicDatabase,
        id: Long,
        newName: String,
    ): Boolean {
        val playlistDao = database.PlaylistDao()
        val result = playlistDao.rename(id, newName)
        if (result) {
            playlistDao.modifyDate(id, currentTimestamp())
            sentPlaylistChangedLocalBoardCast()
        }
        return result
    }

    fun amendPlaylist(
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
            sentPlaylistChangedLocalBoardCast()
        }
        return lines
    }

    fun removeSongFromPlaylist(
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

    fun swapSongFromPlaylist(
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

    fun moveSongFromPlaylist(
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

    fun deletePlaylist(
        database: MusicDatabase,
        id: Long,
    ): Boolean {
        val playlistDao = database.PlaylistDao()

        val playlistEntity = playlistDao.id(id)
        return if (playlistEntity != null) {
            playlistDao.delete(playlistEntity) == 1
        } else {
            false
        }.also {
            sentPlaylistChangedLocalBoardCast()
        }
    }

    private fun currentTimestamp(): Long = System.currentTimeMillis() / 1000
}