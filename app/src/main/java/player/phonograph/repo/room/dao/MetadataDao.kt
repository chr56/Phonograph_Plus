/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.Metadata
import player.phonograph.repo.room.entity.Tables
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class MetadataDao {

    @Query("SELECT * FROM ${Tables.METADATA}")
    abstract fun all(): List<Metadata>

    @Query("SELECT * FROM ${Tables.METADATA} WHERE ${Columns.METADATA_KEY} = :key LIMIT 1")
    abstract fun getByKey(key: String): Metadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrReplace(metadata: Metadata)

    @Query("DELETE FROM ${Tables.METADATA} WHERE ${Columns.METADATA_KEY} = :key")
    abstract fun deleteByKey(key: String)

    @Query("DELETE FROM ${Tables.METADATA}")
    abstract fun deleteAll()
}