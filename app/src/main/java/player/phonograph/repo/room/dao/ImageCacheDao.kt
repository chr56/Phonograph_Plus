/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.ImageCacheEntity
import player.phonograph.repo.room.entity.ImageCacheEntity.CacheDomain
import player.phonograph.repo.room.entity.Tables
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class ImageCacheDao {

    @Query(
        "SELECT * FROM ${Tables.IMAGE_CACHE} " +
                "WHERE ${Columns.CACHE_DOMAIN} = :domain " +
                "AND ${Columns.CACHE_ID} = :id " +
                "AND ${Columns.CACHE_SOURCE} = :source " +
                "LIMIT 1"
    )
    abstract fun obtain(@CacheDomain domain: Int, id: Long, source: Int): ImageCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun refresh(cache: ImageCacheEntity): Long

    @Query(
        "DELETE FROM ${Tables.IMAGE_CACHE} " +
                "WHERE ${Columns.CACHE_DOMAIN} = :domain " +
                "AND ${Columns.CACHE_ID} = :id " +
                "AND ${Columns.CACHE_SOURCE} = :source"
    )
    abstract fun delete(@CacheDomain domain: Int, id: Long, source: Int): Int

    @Query("DELETE FROM ${Tables.IMAGE_CACHE}")
    abstract fun deleteAll(): Int
}
