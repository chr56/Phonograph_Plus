/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.converter

import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.playlist.DatabasePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.PlaylistEntity
import androidx.room.TypeConverter

object EntityConverter {

    //region Songs
    @TypeConverter
    fun fromSongModel(song: Song): MediastoreSongEntity = MediastoreSongEntity(
        mediastorId = song.id,
        path = song.data,
        duration = song.duration,
        dateAdded = song.dateAdded,
        dateModified = song.dateModified,
        title = song.title,
        albumId = song.albumId,
        album = song.albumName ?: "",
        artistId = song.artistId,
        artist = song.artistName ?: "",
        year = song.year,
        track = song.trackNumber,
        albumArtist = song.albumArtistName ?: "",
        composer = song.composer ?: "",
    )

    @TypeConverter
    fun toSongModel(entity: MediastoreSongEntity): Song = Song(
        id = entity.mediastorId,
        title = entity.title,
        trackNumber = entity.track,
        year = entity.year,
        duration = entity.duration,
        data = entity.path,
        dateAdded = entity.dateAdded,
        dateModified = entity.dateModified,
        albumId = entity.albumId,
        albumName = entity.album,
        artistId = entity.artistId,
        artistName = entity.artist,
        albumArtistName = entity.albumArtist,
        composer = entity.composer,
    )
    //endregion

    //region Playlist
    @TypeConverter
    fun toPlaylist(playlist: PlaylistEntity): Playlist =
        Playlist(
            name = playlist.name,
            location = DatabasePlaylistLocation(playlist.id),
            dateAdded = playlist.dateAdded,
            dateModified = playlist.dateModified,
            iconRes = R.drawable.ic_queue_music_white_24dp
        )
    //endregion
}