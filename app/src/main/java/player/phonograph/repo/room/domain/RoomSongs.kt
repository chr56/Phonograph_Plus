/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.model.Song
import player.phonograph.model.repo.loader.ISongs
import player.phonograph.repo.room.converter.EntityConverter
import player.phonograph.repo.room.dao.RoomSortOrder.defaultAlbumSortMode
import player.phonograph.repo.room.dao.RoomSortOrder.defaultArtistSortMode
import android.content.Context

object RoomSongs : RoomLoader(), ISongs {

    override suspend fun all(context: Context): List<Song> =
        db.SongQueryDao().all(songSortMode(context)).map(EntityConverter::toSongModel)

    override suspend fun id(context: Context, id: Long): Song? =
        db.SongQueryDao().id(id)?.let(EntityConverter::toSongModel)

    override suspend fun path(context: Context, path: String): Song? =
        db.SongQueryDao().path(path)?.let(EntityConverter::toSongModel)

    override suspend fun artist(context: Context, artistId: Long): List<Song> =
        db.ArtistQueryDao().artistSongs(artistId, defaultArtistSortMode)?.songEntities.orEmpty().map(EntityConverter::toSongModel)

    override suspend fun album(context: Context, albumId: Long): List<Song> =
        db.AlbumQueryDao().albumSongs(albumId, defaultAlbumSortMode)?.songEntities.orEmpty().map(EntityConverter::toSongModel)

    override suspend fun since(context: Context, timestamp: Long, useModifiedDate: Boolean): List<Song> =
        db.SongQueryDao().since(timestamp, useModifiedDate).map(EntityConverter::toSongModel)

    override suspend fun lastest(context: Context): Song? =
        db.SongQueryDao().latest()?.let(EntityConverter::toSongModel)

    override suspend fun total(context: Context): Int = db.SongQueryDao().total()

    override suspend fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song> =
        db.SongQueryDao().searchByPath("%$path%").map(EntityConverter::toSongModel)

    override suspend fun searchByTitle(context: Context, title: String): List<Song> =
        db.SongQueryDao().searchByTitle("%$title%").map(EntityConverter::toSongModel)

}
