/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.repo.room.entity.Columns
import player.phonograph.repo.room.entity.LinkageSongAndArtist
import player.phonograph.repo.room.entity.Tables.LINKAGE_ARTIST_SONG
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class RelationshipArtistSongDao {

    @Query("SELECT * from $LINKAGE_ARTIST_SONG where ${Columns.ARTIST_ID} = :artistId")
    abstract fun artist(artistId: Long): List<LinkageSongAndArtist>

    @Query("SELECT * from $LINKAGE_ARTIST_SONG where ${Columns.MEDIASTORE_ID} = :songId")
    abstract fun song(songId: Long): List<LinkageSongAndArtist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun override(linkage: LinkageSongAndArtist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun override(linkages: List<LinkageSongAndArtist>)

    @Delete
    abstract fun remove(linkage: LinkageSongAndArtist): Int

    @Delete
    abstract fun remove(linkages: List<LinkageSongAndArtist>): Int

    @Query("DELETE FROM $LINKAGE_ARTIST_SONG")
    abstract suspend fun deleteAll(): Int
}