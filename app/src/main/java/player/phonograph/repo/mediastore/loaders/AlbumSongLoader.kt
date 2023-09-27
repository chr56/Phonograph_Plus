/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.loaders

import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.internal.intoSongs
import player.phonograph.repo.mediastore.internal.querySongs
import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns

object AlbumSongLoader {
    fun id(context: Context, albumId: Long): List<Song> =
        querySongs(context, "${AudioColumns.ALBUM_ID}=?", arrayOf(albumId.toString()), AudioColumns.TRACK).intoSongs()

    fun Album.allSongs(context: Context) = id(context, id)
    fun List<Album>.allSongs(context: Context): List<Song> = this.flatMap { it.allSongs(context) }
}