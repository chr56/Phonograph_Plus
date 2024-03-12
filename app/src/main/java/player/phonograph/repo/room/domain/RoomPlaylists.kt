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
import player.phonograph.repo.loader.IPlaylists
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.converter.MediastoreSongConverter
import player.phonograph.repo.room.dao.PlaylistDao
import player.phonograph.repo.room.dao.PlaylistSongDao
import player.phonograph.repo.room.entity.PlaylistEntity
import android.content.Context

object RoomPlaylists : IPlaylists {

    override suspend fun all(context: Context): List<Playlist> {
        val playlistDao = playlistDao()
        return playlistDao.all().map(::convertPlaylist)
    }

    override suspend fun of(context: Context, location: PlaylistLocation): Playlist? =
        if (location is DatabasePlaylistLocation) {
            val playlistDao = playlistDao()
            playlistDao.id(location.databaseId)?.let(::convertPlaylist)
        } else {
            null
        }

    override suspend fun songs(context: Context, location: PlaylistLocation): List<PlaylistSong> {
        val id = (location as? DatabasePlaylistLocation)?.databaseId ?: return emptyList()
        val playlistSongDao = playlistSongDao()
        val songs = playlistSongDao.songs(id)
        return songs.map { item ->
            if (item != null) {
                val song = item.songEntity.let(MediastoreSongConverter::toSongModel)
                PlaylistSong(song, item.playlistId, item.position.toLong())
            } else {
                PlaylistSong(Song.deleted(context.getString(R.string.deleted), ""), id, -1)
            }
        }
    }

    override suspend fun contains(context: Context, location: PlaylistLocation, songId: Long): Boolean {
        if (location !is DatabasePlaylistLocation) return false
        val playlistSongDao = playlistSongDao()
        return playlistSongDao.count(location.databaseId, songId) > 0
    }

    override suspend fun named(context: Context, name: String): Playlist? {
        val playlistDao = playlistDao()
        return playlistDao.all().find { it.name == name }?.let(::convertPlaylist)
    }

    override suspend fun exists(context: Context, location: PlaylistLocation): Boolean =
        if (location is DatabasePlaylistLocation) {
            val playlistDao = playlistDao()
            playlistDao.id(location.databaseId) != null
        } else {
            false
        }

    override suspend fun searchByName(context: Context, query: String): List<Playlist> {
        val playlistDao = playlistDao()
        return playlistDao.all().filter { it.name.contains(query) }.map(::convertPlaylist)
    }

    private fun playlistDao(): PlaylistDao {
        val database = MusicDatabase.koinInstance
        return database.PlaylistDao()
    }

    private fun playlistSongDao(): PlaylistSongDao {
        val database = MusicDatabase.koinInstance
        return database.PlaylistSongDao()
    }

    private fun convertPlaylist(playlist: PlaylistEntity): Playlist =
        Playlist(
            name = playlist.name,
            location = DatabasePlaylistLocation(playlist.id),
            dateAdded = playlist.dateAdded,
            dateModified = playlist.dateModified,
            iconRes = R.drawable.ic_queue_music_white_24dp
        )
}