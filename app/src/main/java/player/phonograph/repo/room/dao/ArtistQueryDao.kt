/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.model.sort.SortMode
import player.phonograph.repo.room.dao.RoomSortOrder.roomArtistQuerySortOrder
import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.Columns.ALBUM
import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import player.phonograph.repo.room.entity.Columns.ARTIST
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Columns.MEDIASTORE_ID
import player.phonograph.repo.room.entity.Columns.TRACK
import player.phonograph.repo.room.entity.Columns.YEAR
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.Tables.ALBUMS
import player.phonograph.repo.room.entity.Tables.ARTISTS
import player.phonograph.repo.room.entity.Tables.LINKAGE_ARTIST_ALBUM
import player.phonograph.repo.room.entity.Tables.LINKAGE_ARTIST_SONG
import player.phonograph.repo.room.entity.Tables.MEDIASTORE_SONGS
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
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

    @Query(
        "SELECT $MEDIASTORE_SONGS.* from $LINKAGE_ARTIST_SONG inner join $MEDIASTORE_SONGS " +
                "on $LINKAGE_ARTIST_SONG.$MEDIASTORE_ID = $MEDIASTORE_SONGS.$MEDIASTORE_ID " +
                "where $LINKAGE_ARTIST_SONG.$ARTIST_ID = :artistId " +
                "order by $ALBUM, $TRACK"
    )
    abstract suspend fun artistSongs(artistId: Long): List<MediastoreSongEntity>

    @Query("SELECT COUNT(${MEDIASTORE_ID}) from $LINKAGE_ARTIST_SONG where $ARTIST_ID = :artistId")
    abstract suspend fun artistSongCount(artistId: Long): Int

    @Query(
        "SELECT $ALBUMS.* from $LINKAGE_ARTIST_ALBUM inner join $ALBUMS " +
                "on $LINKAGE_ARTIST_ALBUM.$ALBUM_ID = $ALBUMS.$ALBUM_ID " +
                "where $LINKAGE_ARTIST_ALBUM.$ARTIST_ID = :artistId " +
                "order by $YEAR desc, $ALBUM"
    )
    abstract suspend fun artistAlbums(artistId: Long): List<AlbumEntity>

    @Query("SELECT COUNT(${ALBUM_ID}) from $LINKAGE_ARTIST_ALBUM where $ARTIST_ID = :artistId")
    abstract suspend fun artistAlbumCount(artistId: Long): Int

    //region Raw
    @RawQuery
    protected abstract suspend fun query(query: SupportSQLiteQuery): List<ArtistEntity>
    //endregion
}
