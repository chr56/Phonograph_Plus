/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.SongAndArtistLinkage
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface RelationShipDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun override(linkage: SongAndArtistLinkage)

    @Delete
    fun remove(linkage: SongAndArtistLinkage)
}