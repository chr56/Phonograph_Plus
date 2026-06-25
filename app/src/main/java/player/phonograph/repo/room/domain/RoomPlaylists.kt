/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.PlaylistSong
import player.phonograph.model.playlist.DatabasePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.PlaylistLocation
import player.phonograph.model.repo.loader.IPlaylists
import player.phonograph.repo.room.converter.EntityConverter
import android.content.Context

object RoomPlaylists : RoomLoader(), IPlaylists {

    override suspend fun all(context: Context): List<Playlist> {
        val playlistQueryDao = db.PlaylistQueryDao()
        return playlistQueryDao.all().map(EntityConverter::toPlaylist)
    }

    override suspend fun of(context: Context, location: PlaylistLocation): Playlist? =
        if (location is DatabasePlaylistLocation) {
            val playlistQueryDao = db.PlaylistQueryDao()
            playlistQueryDao.id(location.databaseId)?.let(EntityConverter::toPlaylist)
        } else {
            null
        }

    override suspend fun songs(context: Context, location: PlaylistLocation): List<PlaylistSong> {
        val id = (location as? DatabasePlaylistLocation)?.databaseId ?: return emptyList()
        val playlistSongQueryDao = db.PlaylistSongQueryDao()
        val songs = playlistSongQueryDao.songs(id)
        return songs.map { item ->
            PlaylistSong(item.songEntity.let(EntityConverter::toSongModel), item.playlistId, item.position.toLong())
        }
    }

    override suspend fun contains(context: Context, location: PlaylistLocation, songId: Long): Boolean {
        if (location !is DatabasePlaylistLocation) return false
        val playlistSongQueryDao = db.PlaylistSongQueryDao()
        return playlistSongQueryDao.count(location.databaseId, songId) > 0
    }

    override suspend fun named(context: Context, name: String): Playlist? {
        val playlistQueryDao = db.PlaylistQueryDao()
        return playlistQueryDao.all().find { it.name == name }?.let(EntityConverter::toPlaylist)
    }

    override suspend fun exists(context: Context, location: PlaylistLocation): Boolean =
        if (location is DatabasePlaylistLocation) {
            val playlistQueryDao = db.PlaylistQueryDao()
            playlistQueryDao.id(location.databaseId) != null
        } else {
            false
        }

    override suspend fun searchByName(context: Context, query: String): List<Playlist> {
        val playlistQueryDao = db.PlaylistQueryDao()
        return playlistQueryDao.all().filter { it.name.contains(query) }.map(EntityConverter::toPlaylist)
    }

}
