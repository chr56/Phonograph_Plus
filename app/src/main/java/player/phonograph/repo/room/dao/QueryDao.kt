/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.dao

import player.phonograph.model.sort.SortMode
import player.phonograph.repo.room.dao.RoomSortOrder.roomAlbumQuerySortOrder
import player.phonograph.repo.room.dao.RoomSortOrder.roomArtistQuerySortOrder
import player.phonograph.repo.room.dao.RoomSortOrder.roomSongQuerySortOrder
import player.phonograph.repo.room.entity.AlbumEntity
import player.phonograph.repo.room.entity.ArtistEntity
import player.phonograph.repo.room.entity.Columns.ALBUM
import player.phonograph.repo.room.entity.Columns.ALBUM_ID
import player.phonograph.repo.room.entity.Columns.ARTIST
import player.phonograph.repo.room.entity.Columns.ARTIST_ID
import player.phonograph.repo.room.entity.Columns.MEDIASTORE_ID
import player.phonograph.repo.room.entity.Columns.PATH
import player.phonograph.repo.room.entity.Columns.TITLE
import player.phonograph.repo.room.entity.MediastoreSongEntity
import player.phonograph.repo.room.entity.Tables.ALBUMS
import player.phonograph.repo.room.entity.Tables.ARTISTS
import player.phonograph.repo.room.entity.Tables.MEDIASTORE_SONGS
import player.phonograph.repo.room.entity.derived.AlbumWithSongs
import player.phonograph.repo.room.entity.derived.ArtistWithAlbums
import player.phonograph.repo.room.entity.derived.ArtistWithAll
import player.phonograph.repo.room.entity.derived.ArtistWithSongs
import player.phonograph.repo.room.entity.derived.SongWithArtists
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
abstract class QueryDao {

    // Search Songs


    fun songsWithTitle(title: String, sortMode: SortMode): List<MediastoreSongEntity> = searchSongEntity(
        SimpleSQLiteQuery(
            "SELECT * from $MEDIASTORE_SONGS where $TITLE like ? order by ${roomSongQuerySortOrder(sortMode)}",
            arrayOf<Any>("%$title%")
        )
    )

    fun songsWithAlbum(albumName: String, sortMode: SortMode): List<MediastoreSongEntity> = searchSongEntity(
        SimpleSQLiteQuery(
            "SELECT * from $MEDIASTORE_SONGS where $ALBUM like ? order by ${roomSongQuerySortOrder(sortMode)}",
            arrayOf<Any>(albumName)
        )
    )

    fun songsWithArtist(artistName: String, sortMode: SortMode): List<MediastoreSongEntity> = searchSongEntity(
        SimpleSQLiteQuery(
            "SELECT * from $MEDIASTORE_SONGS where $ARTIST like ? order by ${roomSongQuerySortOrder(sortMode)}",
            arrayOf<Any>(artistName)
        )
    )

    fun songsWithPath(path: String, sortMode: SortMode): List<MediastoreSongEntity> = searchSongEntity(
        SimpleSQLiteQuery(
            "SELECT * from $MEDIASTORE_SONGS where $PATH like ? order by ${roomSongQuerySortOrder(sortMode)}",
            arrayOf<Any>(path)
        )
    )

    // Search Albums

    fun albumsWithName(albumName: String, sortMode: SortMode): List<AlbumEntity> = searchAlbumEntity(
        SimpleSQLiteQuery(
            "SELECT * from $ALBUMS where $ALBUM like ? order by ${roomAlbumQuerySortOrder(sortMode)}",
            arrayOf<Any>("%$albumName%")
        )
    )

    // Search Artist

    fun artistsWithName(artistName: String, sortMode: SortMode): List<ArtistEntity> = searchArtistEntity(
        SimpleSQLiteQuery(
            "SELECT * from $ARTISTS where $ARTIST like ? order by ${roomArtistQuerySortOrder(sortMode)}",
            arrayOf<Any>("%$artistName%")
        )
    )


    // Relationship


    fun artistSongs(artistId: Long, sortMode: SortMode): ArtistWithSongs = rawArtistWithSongs(
        SimpleSQLiteQuery(
            "SELECT * from $ARTISTS where $ARTIST_ID = ? order by ${roomArtistQuerySortOrder(sortMode)}",
            arrayOf<Any>(artistId)
        )
    )

    fun artistAlbums(artistId: Long, sortMode: SortMode): ArtistWithAlbums = rawArtistWithAlbums(
        SimpleSQLiteQuery(
            "SELECT * from $ARTISTS where $ARTIST_ID = ? order by ${roomArtistQuerySortOrder(sortMode)}",
            arrayOf<Any>(artistId)
        )
    )


    fun artistDetails(artistId: Long, sortMode: SortMode): ArtistWithAll = rawArtistWithAll(
        SimpleSQLiteQuery(
            "SELECT * from $ARTISTS where $ARTIST_ID = :? order by ${roomArtistQuerySortOrder(sortMode)}",
            arrayOf<Any>(artistId)
        )
    )

    fun albumSongs(albumId: Long, sortMode: SortMode): AlbumWithSongs = rawAlbumWithSongs(
        SimpleSQLiteQuery(
            "SELECT * from $ALBUMS where $ALBUM_ID = ? order by ${roomAlbumQuerySortOrder(sortMode)}",
            arrayOf<Any>(albumId)
        )
    )


    fun artistsOfSong(songId: Long, sortMode: SortMode): SongWithArtists = rawSongWithArtists(
        SimpleSQLiteQuery(
            "SELECT * from $MEDIASTORE_SONGS where $MEDIASTORE_ID = ? order by ${roomSongQuerySortOrder(sortMode)}",
            arrayOf<Any>(songId)
        )
    )


    fun artistsOfAllSongs(sortMode: SortMode): List<SongWithArtists> = rawSongWithArtistsList(
        SimpleSQLiteQuery(
            "SELECT * from $MEDIASTORE_SONGS order by ${roomSongQuerySortOrder(sortMode)}",
        )
    )


    fun allArtistSongs(sortMode: SortMode): List<ArtistWithSongs> = rawArtistWithSongsList(
        SimpleSQLiteQuery(
            "SELECT * from $ARTISTS order by ${roomArtistQuerySortOrder(sortMode)}",
        )
    )

    fun allArtistAlbums(sortMode: SortMode): List<ArtistWithAlbums> = rawArtistWithAlbumsList(
        SimpleSQLiteQuery(
            "SELECT * from $ARTISTS order by ${roomArtistQuerySortOrder(sortMode)}",
        )
    )

    fun allArtistDetails(sortMode: SortMode): List<ArtistWithAll> = rawArtistWithAllList(
        SimpleSQLiteQuery(
            "SELECT * from $ARTISTS order by ${roomArtistQuerySortOrder(sortMode)}",
        )
    )

    //  RawSearch
    @RawQuery
    protected abstract fun searchSongEntity(query: SupportSQLiteQuery): List<MediastoreSongEntity>
    @RawQuery
    protected abstract fun searchAlbumEntity(query: SupportSQLiteQuery): List<AlbumEntity>
    @RawQuery
    protected abstract fun searchArtistEntity(query: SupportSQLiteQuery): List<ArtistEntity>


    // RawRelationship
    @Transaction
    @RawQuery
    protected abstract fun rawAlbumWithSongs(query: SupportSQLiteQuery): AlbumWithSongs
    @Transaction
    @RawQuery
    protected abstract fun rawArtistWithSongs(query: SupportSQLiteQuery): ArtistWithSongs
    @Transaction
    @RawQuery
    protected abstract fun rawArtistWithAlbums(query: SupportSQLiteQuery): ArtistWithAlbums
    @Transaction
    @RawQuery
    protected abstract fun rawArtistWithAll(query: SupportSQLiteQuery): ArtistWithAll
    @Transaction
    @RawQuery
    protected abstract fun rawSongWithArtists(query: SupportSQLiteQuery): SongWithArtists
    @Transaction
    @RawQuery
    protected abstract fun rawSongWithArtistsList(query: SupportSQLiteQuery): List<SongWithArtists>
    @Transaction
    @RawQuery
    protected abstract fun rawArtistWithSongsList(query: SupportSQLiteQuery): List<ArtistWithSongs>
    @Transaction
    @RawQuery
    protected abstract fun rawArtistWithAlbumsList(query: SupportSQLiteQuery): List<ArtistWithAlbums>
    @Transaction
    @RawQuery
    protected abstract fun rawArtistWithAllList(query: SupportSQLiteQuery): List<ArtistWithAll>

}