/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.loaders

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.internal.catalogAlbums
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns
import kotlinx.coroutines.runBlocking

object ArtistAlbumLoader {
    fun id(context: Context, artistId: Long): List<Album> =
        querySongs(context, "${AudioColumns.ARTIST_ID}=?", arrayOf(artistId.toString()), null)
            .intoSongs()
            .toAlbumList()

    fun Artist.allAlbums(context: Context): List<Album> = id(context, id)

    private fun List<Song>.toAlbumList(): List<Album> = runBlocking { catalogAlbums(this@toAlbumList).await() }
}