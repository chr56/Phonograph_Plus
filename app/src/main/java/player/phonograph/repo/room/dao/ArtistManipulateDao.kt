/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.Tables.ARTISTS
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class ArtistManipulateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(artist: ArtistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun update(artists: List<ArtistEntity>): LongArray

    suspend fun updateCounter(queryDao: ArtistQueryDao, artistId: Long, songCount: Int? = null, albumCount: Int? = null): Boolean {
        val original = queryDao.id(artistId) ?: return false
        val updated = original.copy(
            songCount = songCount ?: original.songCount,
            albumCount = albumCount ?: original.albumCount
        )
        return update(updated) == artistId
    }

    suspend fun delete(queryDao: ArtistQueryDao, artistId: Long): Boolean {
        return delete(queryDao.id(artistId) ?: return false) > 0
    }

    @Delete
    abstract suspend fun delete(artist: ArtistEntity): Int

    @Delete
    abstract suspend fun delete(artists: List<ArtistEntity>): Int

    @Query("DELETE FROM $ARTISTS")
    abstract suspend fun deleteAll(): Int
}
