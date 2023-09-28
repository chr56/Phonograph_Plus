/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.loaders

import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns

object ArtistSongLoader {
    fun id(context: Context, artistId: Long): List<Song> =
        querySongs(context, "${AudioColumns.ARTIST_ID}=?", arrayOf(artistId.toString()), null).intoSongs()

    fun Artist.allSongs(context: Context): List<Song> = id(context, id)

    fun List<Artist>.allSongs(context: Context): List<Song> = this.flatMap { it.allSongs(context) }
}