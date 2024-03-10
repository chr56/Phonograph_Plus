/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.converter

import player.phonograph.model.Song
import player.phonograph.repo.room.entity.MediastoreSongEntity
import androidx.room.TypeConverter

object MediastoreSongConverter {

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
}