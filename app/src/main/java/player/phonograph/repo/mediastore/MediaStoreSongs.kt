/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.Song
import player.phonograph.model.repo.loader.ISongs
import player.phonograph.repo.mediastore.loaders.AlbumSongLoader
import player.phonograph.repo.mediastore.loaders.ArtistSongLoader
import player.phonograph.repo.mediastore.loaders.GenreLoader
import player.phonograph.repo.mediastore.loaders.SongLoader
import android.content.Context

object MediaStoreSongs : ISongs {

    override suspend fun all(context: Context): List<Song> = SongLoader.all(context)

    override suspend fun id(context: Context, id: Long): Song? = SongLoader.id(context, id)

    override suspend fun path(context: Context, path: String): Song? = SongLoader.path(context, path)

    override suspend fun artist(context: Context, artistId: Long): List<Song> = ArtistSongLoader.id(context, artistId)

    override suspend fun album(context: Context, albumId: Long): List<Song> = AlbumSongLoader.id(context, albumId)

    override suspend fun genres(context: Context, genreId: Long): List<Song> = GenreLoader.genreSongs(context, genreId)

    /**
     * @param withoutPathFilter true if disable path filter
     */
    override suspend fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song> =
        SongLoader.searchByPath(context, path, withoutPathFilter)

    override suspend fun searchByTitle(context: Context, title: String): List<Song> =
        SongLoader.searchByTitle(context, title)

    override suspend fun since(context: Context, timestamp: Long, useModifiedDate: Boolean): List<Song> =
        SongLoader.since(context, timestamp, useModifiedDate)

}