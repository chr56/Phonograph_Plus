/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.room.loader

import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.repo.loader.ISongs
import player.phonograph.repo.room.defaultAlbumSortMode
import player.phonograph.repo.room.defaultArtistSortMode
import player.phonograph.repo.room.defaultSongSortMode
import player.phonograph.repo.room.songSortMode
import android.content.Context

object RoomSongs : BaseLoader(), ISongs {

    override fun all(context: Context): List<Song> =
        db.SongDao().all(songSortMode(context)).convertSongs()

    override fun id(context: Context, id: Long): Song =
        db.SongDao().id(id).convert()

    override fun path(context: Context, path: String): Song =
        db.SongDao().path(path).convert()

    override fun artist(context: Context, artistId: Long): List<Song> =
        db.QueryDao().artistSongs(artistId, defaultArtistSortMode).songEntities.convertSongs()

    override fun album(context: Context, albumId: Long): List<Song> =
        db.QueryDao().albumSongs(albumId, defaultAlbumSortMode).songEntities.convertSongs()

    override fun since(context: Context, timestamp: Long, useModifiedDate: Boolean): List<Song> =
        db.SongDao().since(timestamp, useModifiedDate).convertSongs()

    override fun genres(context: Context, genreId: Long): List<Song> = emptyList() //todo

    override fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song> =
        db.QueryDao().songsWithPath(path, defaultSongSortMode).convertSongs()

    override fun searchByTitle(context: Context, title: String): List<Song> =
        db.QueryDao().songsWithTitle(title, defaultSongSortMode).convertSongs()

    override fun searchByFileEntity(context: Context, file: FileEntity.File): Song {
        return if (file.id > 0) db.SongDao().id(file.id).convert()
        else searchByPath(context, file.location.sqlPattern, true).firstOrNull() ?: Song.EMPTY_SONG
    }
}