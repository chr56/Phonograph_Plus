/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.model.sort.SortMode
import player.phonograph.repo.room.dao.RoomSortOrder.roomArtistQuerySortOrder
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import player.phonograph.repo.room.entity.Columns.ARTIST
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Columns.MEDIASTORE_ID
import player.phonograph.repo.room.entity.Tables.ARTISTS
import player.phonograph.repo.room.entity.Tables.LINKAGE_ARTIST_ALBUM
import player.phonograph.repo.room.entity.Tables.LINKAGE_ARTIST_SONG
import player.phonograph.repo.room.entity.derived.ArtistWithAlbums
import player.phonograph.repo.room.entity.derived.ArtistWithAll
import player.phonograph.repo.room.entity.derived.ArtistWithSongs
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
abstract class ArtistQueryDao {

    suspend fun all(sortMode: SortMode): List<ArtistEntity> = query(
        SimpleSQLiteQuery(
            "SELECT * from $ARTISTS order by ${roomArtistQuerySortOrder(sortMode)}"
        )
    )

    @Query("SELECT * from $ARTISTS where $ARTIST_ID = :id")
    abstract suspend fun id(id: Long): ArtistEntity?

    @Query("SELECT * from $ARTISTS where $ARTIST = :name")
    abstract suspend fun named(name: String): ArtistEntity?

    @Query("SELECT * from $ARTISTS where $ARTIST in (:names)")
    abstract suspend fun named(names: Collection<String>): List<ArtistEntity>

    @Query("SELECT * from $ARTISTS where $ARTIST like :name")
    abstract suspend fun searchByName(name: String): List<ArtistEntity>

    @Query("SELECT COUNT(*) from $ARTISTS")
    abstract suspend fun count(): Int

    suspend fun artistSongs(artistId: Long, sortMode: SortMode): ArtistWithSongs? = queryArtistWithSongs(
        SimpleSQLiteQuery(
            "SELECT * from $ARTISTS where $ARTIST_ID = ? order by ${roomArtistQuerySortOrder(sortMode)}",
            arrayOf<Any>(artistId)
        )
    )

    @Query("SELECT COUNT(${MEDIASTORE_ID}) from $LINKAGE_ARTIST_SONG where $ARTIST_ID = :artistId")
    abstract suspend fun artistSongCount(artistId: Long): Int

    suspend fun artistAlbums(artistId: Long, sortMode: SortMode): ArtistWithAlbums? = queryArtistWithAlbums(
        SimpleSQLiteQuery(
            "SELECT * from $ARTISTS where $ARTIST_ID = ? order by ${roomArtistQuerySortOrder(sortMode)}",
            arrayOf<Any>(artistId)
        )
    )

    @Query("SELECT COUNT(${ALBUM_ID}) from $LINKAGE_ARTIST_ALBUM where $ARTIST_ID = :artistId")
    abstract suspend fun artistAlbumCount(artistId: Long): Int

    suspend fun artistDetails(artistId: Long, sortMode: SortMode): ArtistWithAll? = queryArtistWithAll(
        SimpleSQLiteQuery(
            "SELECT * from $ARTISTS where $ARTIST_ID = ? order by ${roomArtistQuerySortOrder(sortMode)}",
            arrayOf<Any>(artistId)
        )
    )

    //region Raw
    @RawQuery
    protected abstract suspend fun query(query: SupportSQLiteQuery): List<ArtistEntity>

    @Transaction
    @RawQuery
    protected abstract suspend fun queryArtistWithSongs(query: SupportSQLiteQuery): ArtistWithSongs?

    @Transaction
    @RawQuery
    protected abstract suspend fun queryArtistWithAlbums(query: SupportSQLiteQuery): ArtistWithAlbums?

    @Transaction
    @RawQuery
    protected abstract suspend fun queryArtistWithAll(query: SupportSQLiteQuery): ArtistWithAll?
    //endregion
}
