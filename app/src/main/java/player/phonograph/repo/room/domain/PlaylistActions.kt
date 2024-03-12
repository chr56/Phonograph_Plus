/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.Song
import player.phonograph.repo.room.SongDatabase
import player.phonograph.repo.room.entity.PlaylistEntity
import player.phonograph.repo.room.entity.PlaylistSongEntity

@Suppress("MemberVisibilityCanBePrivate")
object PlaylistActions {

    fun createPlaylist(
        database: SongDatabase,
        name: String,
        songs: Collection<Song>,
    ): Boolean {
        val id = createPlaylist(database, name)
        return if (id > 0) {
            amendPlaylist(database, id, songs) == songs.size
        } else {
            false
        }
    }

    fun containsSongFromPlaylist(
        database: SongDatabase,
        playlistId: Long,
        song: Song,
    ): Boolean {
        val playlistSongDao = database.PlaylistSongDao()
        return playlistSongDao.count(playlistId, song.id) > 0
    }


    fun createPlaylist(
        database: SongDatabase,
        name: String,
    ): Long {
        val playlistDao = database.PlaylistDao()
        return playlistDao.insert(PlaylistEntity(name = name))
    }

    fun renamePlaylist(
        database: SongDatabase,
        id: Long,
        newName: String,
    ): Boolean {
        val playlistDao = database.PlaylistDao()
        return playlistDao.rename(id, newName)
    }

    fun amendPlaylist(
        database: SongDatabase,
        id: Long,
        songs: Collection<Song>,
    ): Int {
        val playlistSongDao = database.PlaylistSongDao()
        val indexOffset = playlistSongDao.maximumIndexOf(id) + 1

        val entities = songs.mapIndexed { num, song ->
            PlaylistSongEntity(mediastoreId = song.id, path = song.data, playlistId = id, index = num + indexOffset)
        }

        return playlistSongDao.insert(entities).size
    }

    fun removeSongFromPlaylist(
        database: SongDatabase,
        playlistId: Long,
        songId: Long,
        position: Int,
    ): Boolean {
        val playlistSongDao = database.PlaylistSongDao()
        return playlistSongDao.removeItem(playlistId, songId, position)
    }

    fun swapSongFromPlaylist(
        database: SongDatabase,
        playlistId: Long,
        positionA: Int,
        positionB: Int,
    ): Boolean {
        val playlistSongDao = database.PlaylistSongDao()
        return playlistSongDao.swap(playlistId, positionA, positionB)
    }

    fun moveSongFromPlaylist(
        database: SongDatabase,
        playlistId: Long,
        from: Int,
        to: Int,
    ): Boolean {
        val playlistSongDao = database.PlaylistSongDao()
        return playlistSongDao.move(playlistId, from, to)
    }

    fun deletePlaylist(
        database: SongDatabase,
        id: Long,
    ): Boolean {
        val playlistDao = database.PlaylistDao()

        val playlistEntity = playlistDao.id(id)
        return if (playlistEntity != null) {
            playlistDao.delete(playlistEntity) == 1
        } else {
            false
        }
    }
}