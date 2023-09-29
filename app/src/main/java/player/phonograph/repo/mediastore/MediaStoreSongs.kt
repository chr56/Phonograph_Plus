/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.repo.loader.ISongs
import player.phonograph.repo.mediastore.loaders.AlbumSongLoader
import player.phonograph.repo.mediastore.loaders.ArtistSongLoader
import player.phonograph.repo.mediastore.loaders.GenreLoader
import player.phonograph.repo.mediastore.loaders.SongLoader
import android.content.Context

object MediaStoreSongs : ISongs {

    override fun all(context: Context): List<Song> = SongLoader.all(context)

    override fun id(context: Context, id: Long): Song = SongLoader.id(context, id)

    override fun path(context: Context, path: String): Song = SongLoader.path(context, path)

    override fun artist(context: Context, artistId: Long): List<Song> = ArtistSongLoader.id(context, artistId)

    override fun album(context: Context, albumId: Long): List<Song> = AlbumSongLoader.id(context, albumId)

    override fun genres(context: Context, genreId: Long): List<Song> = GenreLoader.genreSongs(context, genreId)

    /**
     * @param withoutPathFilter true if disable path filter
     */
    override fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song> =
        SongLoader.searchByPath(context, path, withoutPathFilter)

    override fun searchByTitle(context: Context, title: String): List<Song> =
        SongLoader.searchByTitle(context, title)

    override fun searchByFileEntity(context: Context, file: FileEntity.File): Song =
        SongLoader.searchByFileEntity(context, file)

    override fun since(context: Context, timestamp: Long, useModifiedDate: Boolean): List<Song> =
        SongLoader.since(context, timestamp, useModifiedDate)

}