/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.PlaylistSongEntity
import player.phonograph.repo.room.entity.Tables
import player.phonograph.repo.room.entity.derived.PlaylistMediastoreSongEntity
import player.phonograph.repo.room.entity.derived.PlaylistWithSongsEntity
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomWarnings
import androidx.room.Transaction

@Dao
abstract class PlaylistSongQueryDao {

    @Transaction
    @Query("SELECT * FROM ${Tables.PLAYLISTS} WHERE ${Columns.PLAYLIST_ID} =:id")
    abstract suspend fun playlist(id: Long): PlaylistWithSongsEntity?

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query(
        "SELECT * FROM ${Tables.MEDIASTORE_SONGS} " +
                "INNER JOIN ${Tables.PLAYLIST_SONGS} " +
                "ON ${Tables.MEDIASTORE_SONGS}.${Columns.MEDIASTORE_ID} = ${Tables.PLAYLIST_SONGS}.${Columns.MEDIASTORE_ID} " +
                "WHERE ${Columns.PLAYLIST_ID} =:playlistId " +
                "ORDER BY ${Columns.POSITION} ASC"
    )
    abstract suspend fun songs(playlistId: Long): List<PlaylistMediastoreSongEntity>

    @Query("SELECT * FROM ${Tables.PLAYLIST_SONGS} WHERE ${Columns.PLAYLIST_ID} =:id")
    abstract suspend fun rawQuery(id: Long): List<PlaylistSongEntity>

    @Query(
        "SELECT COALESCE(COUNT(${Columns.PLAYLIST_SONG_ID}), 0) " +
                "FROM ${Tables.PLAYLIST_SONGS} " +
                "WHERE ${Columns.PLAYLIST_ID} = :playlistId"
    )
    abstract suspend fun size(playlistId: Long): Int

    @Query(
        "SELECT COALESCE(MAX(${Columns.POSITION}), -1) " +
                "FROM ${Tables.PLAYLIST_SONGS} " +
                "WHERE ${Columns.PLAYLIST_ID} = :playlistId"
    )
    abstract suspend fun maximumIndexOf(playlistId: Long): Int

    @Query(
        "SELECT COALESCE(COUNT(${Columns.PLAYLIST_SONG_ID}), 0) " +
                "FROM ${Tables.PLAYLIST_SONGS} " +
                "WHERE ${Columns.PLAYLIST_ID} = :playlistId " +
                "AND ${Columns.MEDIASTORE_ID} = :songId "
    )
    abstract suspend fun count(playlistId: Long, songId: Long): Int

    @Query(
        "SELECT * FROM ${Tables.PLAYLIST_SONGS} " +
                "WHERE ${Columns.PLAYLIST_ID} = :playlistId " +
                "AND ${Columns.POSITION} = :position"
    )
    abstract suspend fun at(playlistId: Long, position: Int): PlaylistSongEntity?
}
