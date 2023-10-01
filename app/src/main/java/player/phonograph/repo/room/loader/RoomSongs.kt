/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.loader

import player.phonograph.model.Song
import player.phonograph.model.repo.loader.ISongs
import player.phonograph.repo.room.defaultAlbumSortMode
import player.phonograph.repo.room.defaultArtistSortMode
import player.phonograph.repo.room.defaultSongSortMode
import player.phonograph.repo.room.songSortMode
import android.content.Context

object RoomSongs : BaseLoader(), ISongs {

    override suspend fun all(context: Context): List<Song> =
        db.SongDao().all(songSortMode(context)).convertSongs()

    override suspend fun id(context: Context, id: Long): Song? =
        db.SongDao().id(id).convert()

    override suspend fun path(context: Context, path: String): Song? =
        db.SongDao().path(path).convert()

    override suspend fun artist(context: Context, artistId: Long): List<Song> =
        db.QueryDao().artistSongs(artistId, defaultArtistSortMode).songEntities.convertSongs()

    override suspend fun album(context: Context, albumId: Long): List<Song> =
        db.QueryDao().albumSongs(albumId, defaultAlbumSortMode).songEntities.convertSongs()

    override suspend fun since(context: Context, timestamp: Long, useModifiedDate: Boolean): List<Song> =
        db.SongDao().since(timestamp, useModifiedDate).convertSongs()

    override suspend fun genres(context: Context, genreId: Long): List<Song> = emptyList() //todo

    override suspend fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song> =
        db.QueryDao().songsWithPath(path, defaultSongSortMode).convertSongs()

    override suspend fun searchByTitle(context: Context, title: String): List<Song> =
        db.QueryDao().songsWithTitle(title, defaultSongSortMode).convertSongs()

}