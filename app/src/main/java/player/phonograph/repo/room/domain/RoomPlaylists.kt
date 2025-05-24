/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.R
import player.phonograph.model.PlaylistSong
import player.phonograph.model.Song
import player.phonograph.model.playlist.DatabasePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.PlaylistLocation
import player.phonograph.model.repo.loader.IPlaylists
import player.phonograph.repo.room.converter.EntityConverter
import android.content.Context

object RoomPlaylists : RoomLoader(), IPlaylists {

    override suspend fun all(context: Context): List<Playlist> {
        val playlistDao = db.PlaylistDao()
        return playlistDao.all().map(EntityConverter::toPlaylist)
    }

    override suspend fun of(context: Context, location: PlaylistLocation): Playlist? =
        if (location is DatabasePlaylistLocation) {
            val playlistDao = db.PlaylistDao()
            playlistDao.id(location.databaseId)?.let(EntityConverter::toPlaylist)
        } else {
            null
        }

    override suspend fun songs(context: Context, location: PlaylistLocation): List<PlaylistSong> {
        val id = (location as? DatabasePlaylistLocation)?.databaseId ?: return emptyList()
        val playlistSongDao = db.PlaylistSongDao()
        val songs = playlistSongDao.songs(id)
        return songs.map { item ->
            if (item != null) {
                val song = item.songEntity.let(EntityConverter::toSongModel)
                PlaylistSong(song, item.playlistId, item.position.toLong())
            } else {
                PlaylistSong(Song.deleted(context.getString(R.string.deleted), ""), id, -1)
            }
        }
    }

    override suspend fun contains(context: Context, location: PlaylistLocation, songId: Long): Boolean {
        if (location !is DatabasePlaylistLocation) return false
        val playlistSongDao = db.PlaylistSongDao()
        return playlistSongDao.count(location.databaseId, songId) > 0
    }

    override suspend fun named(context: Context, name: String): Playlist? {
        val playlistDao = db.PlaylistDao()
        return playlistDao.all().find { it.name == name }?.let(EntityConverter::toPlaylist)
    }

    override suspend fun exists(context: Context, location: PlaylistLocation): Boolean =
        if (location is DatabasePlaylistLocation) {
            val playlistDao = db.PlaylistDao()
            playlistDao.id(location.databaseId) != null
        } else {
            false
        }

    override suspend fun searchByName(context: Context, query: String): List<Playlist> {
        val playlistDao = db.PlaylistDao()
        return playlistDao.all().filter { it.name.contains(query) }.map(EntityConverter::toPlaylist)
    }

}