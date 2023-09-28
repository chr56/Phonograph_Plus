/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room

import player.phonograph.repo.room.entity.SongEntity
import androidx.room.TypeConverter
import player.phonograph.model.Song as SongModel

object Converters {

    @TypeConverter
    fun fromSongModel(song: SongModel): SongEntity {
        // todo
        return SongEntity(
            id = song.id,
            path = song.data,
            dateAdded = song.dateModified,
            dateModified = song.dateModified,
            title = song.title,
            albumId = song.albumId,
            albumName = song.albumName,
            rawArtistId = song.artistId,
            rawArtistName = song.artistName,
            albumArtistName = song.albumArtistName,
            composer = song.composer,
            year = song.year,
            duration = song.duration,
            trackNumber = song.trackNumber
        )
    }

    @TypeConverter
    fun toSongModel(songEntity: SongEntity): SongModel {
        return SongModel(
            id = songEntity.id,
            title = songEntity.title ?: "UNKNOWN",
            trackNumber = songEntity.trackNumber,
            year = songEntity.year,
            duration = songEntity.duration,
            data = songEntity.path,
            dateAdded = songEntity.dateModified,
            dateModified = songEntity.dateModified,
            albumId = songEntity.albumId,
            albumName = songEntity.albumName,
            artistId = songEntity.rawArtistId,
            artistName = songEntity.rawArtistName,
            albumArtistName = songEntity.albumArtistName,
            composer = songEntity.composer,
        )
    }
}