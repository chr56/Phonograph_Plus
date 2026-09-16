/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.service.queue.internal

import player.phonograph.model.Song

internal object QueueSongConverter {

    fun fromSong(song: Song, queueType: Int, orderInQueue: Int): QueueSongEntity =
        QueueSongEntity(
            queueType = queueType,
            orderInQueue = orderInQueue,
            mediastoreId = song.id,
            title = song.title,
            trackNumber = song.trackNumber,
            year = song.year,
            duration = song.duration,
            data = song.data,
            dateAdded = song.dateAdded,
            dateModified = song.dateModified,
            albumId = song.albumId,
            albumName = song.albumName,
            artistId = song.artistId,
            artistName = song.artistName,
            albumArtistName = song.albumArtistName,
            composer = song.composer,
        )

    fun toSong(entity: QueueSongEntity): Song =
        Song(
            id = entity.mediastoreId,
            title = entity.title,
            trackNumber = entity.trackNumber,
            year = entity.year,
            duration = entity.duration,
            data = entity.data,
            dateAdded = entity.dateAdded,
            dateModified = entity.dateModified,
            albumId = entity.albumId,
            albumName = entity.albumName,
            artistId = entity.artistId,
            artistName = entity.artistName,
            albumArtistName = entity.albumArtistName,
            composer = entity.composer,
        )
}