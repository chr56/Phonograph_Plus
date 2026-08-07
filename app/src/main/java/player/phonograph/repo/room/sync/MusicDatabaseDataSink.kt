/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.repo.room.sync

import player.phonograph.repo.room.dao.AlbumManipulateDao
import player.phonograph.repo.room.dao.AlbumQueryDao
import player.phonograph.repo.room.dao.ArtistManipulateDao
import player.phonograph.repo.room.dao.ArtistQueryDao
import player.phonograph.repo.room.dao.GenreManipulateDao
import player.phonograph.repo.room.dao.GenreQueryDao
import player.phonograph.repo.room.dao.RelationshipManipulateDao
import player.phonograph.repo.room.dao.RelationshipQueryDao
import player.phonograph.repo.room.dao.SongManipulateDao
import player.phonograph.repo.room.dao.SongQueryDao

interface MusicDatabaseDataSink {
    fun SongQueryDao(): SongQueryDao
    fun SongManipulateDao(): SongManipulateDao
    fun AlbumQueryDao(): AlbumQueryDao
    fun AlbumManipulateDao(): AlbumManipulateDao
    fun ArtistQueryDao(): ArtistQueryDao
    fun ArtistManipulateDao(): ArtistManipulateDao
    fun GenreQueryDao(): GenreQueryDao
    fun GenreManipulateDao(): GenreManipulateDao
    fun RelationshipQueryDao(): RelationshipQueryDao
    fun RelationshipManipulateDao(): RelationshipManipulateDao
    suspend fun <R> withTransaction(block: suspend () -> R): R
}