/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.Song
import player.phonograph.model.repo.loader.ISongs
import player.phonograph.repo.room.converter.EntityConverter
import player.phonograph.repo.room.dao.RoomSortOrder.defaultAlbumSortMode
import player.phonograph.repo.room.dao.RoomSortOrder.defaultArtistSortMode
import player.phonograph.repo.room.dao.RoomSortOrder.defaultSongSortMode
import android.content.Context

object RoomSongs : RoomLoader(), ISongs {

    override suspend fun all(context: Context): List<Song> =
        db.MediaStoreSongDao().all(songSortMode(context)).map(EntityConverter::toSongModel)

    override suspend fun id(context: Context, id: Long): Song? =
        db.MediaStoreSongDao().id(id)?.let(EntityConverter::toSongModel)

    override suspend fun path(context: Context, path: String): Song? =
        db.MediaStoreSongDao().path(path)?.let(EntityConverter::toSongModel)

    override suspend fun artist(context: Context, artistId: Long): List<Song> =
        db.QueryDao().artistSongs(artistId, defaultArtistSortMode).songEntities.map(EntityConverter::toSongModel)

    override suspend fun album(context: Context, albumId: Long): List<Song> =
        db.QueryDao().albumSongs(albumId, defaultAlbumSortMode).songEntities.map(EntityConverter::toSongModel)

    override suspend fun since(context: Context, timestamp: Long, useModifiedDate: Boolean): List<Song> =
        db.MediaStoreSongDao().since(timestamp, useModifiedDate).map(EntityConverter::toSongModel)

    override suspend fun genres(context: Context, genreId: Long): List<Song> = emptyList() //todo

    override suspend fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song> =
        db.QueryDao().songsWithPath(path, defaultSongSortMode).map(EntityConverter::toSongModel)

    override suspend fun searchByTitle(context: Context, title: String): List<Song> =
        db.QueryDao().songsWithTitle(title, defaultSongSortMode).map(EntityConverter::toSongModel)

}