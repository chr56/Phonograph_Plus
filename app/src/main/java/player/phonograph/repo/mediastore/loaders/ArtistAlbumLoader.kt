/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.loaders

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.repo.mediastore.internal.generateArtistAlbums
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns

object ArtistAlbumLoader {
    suspend fun id(context: Context, artistId: Long): List<Album> =
        querySongs(context, "${AudioColumns.ARTIST_ID}=?", arrayOf(artistId.toString()), null)
            .intoSongs().let { generateArtistAlbums(it) }

    suspend fun Artist.allAlbums(context: Context): List<Album> = id(context, id)

}