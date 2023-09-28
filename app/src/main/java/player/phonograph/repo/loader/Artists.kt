/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.ArtistAlbumLoader
import player.phonograph.repo.mediastore.loaders.ArtistLoader
import player.phonograph.repo.mediastore.loaders.ArtistSongLoader
import android.content.Context

object Artists {

    fun all(context: Context): List<Artist> = ArtistLoader.all(context)

    fun id(context: Context, id: Long): Artist = ArtistLoader.id(context, id)
    fun searchByName(context: Context, query: String) = ArtistLoader.searchByName(context, query)

    fun albums(context: Context, artistId: Long): List<Album> = ArtistAlbumLoader.id(context, artistId)
    fun songs(context: Context, artistId: Long): List<Song> = ArtistSongLoader.id(context, artistId)

}