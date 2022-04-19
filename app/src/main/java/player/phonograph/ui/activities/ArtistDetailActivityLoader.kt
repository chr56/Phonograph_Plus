/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import android.content.Context
import kotlinx.coroutines.*
import player.phonograph.loader.ArtistLoader
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song

class ArtistDetailActivityLoader(var artistId: Long) {
    private val loaderCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    var isRecyclerViewPrepared: Boolean = false
    fun loadDataSet(context: Context, artistCallback: (Artist) -> Unit, songCallback: (List<Song>) -> Unit, albumCallback: (List<Album>) -> Unit) {
        loaderCoroutineScope.launch {

            _artist = ArtistLoader.getArtist(context, artistId)

            val songs: List<Song> = artist.songs
            val albums: List<Album> = artist.albums ?: emptyList()

            while (!isRecyclerViewPrepared) yield() // wait until ready
            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) {
                    artistCallback(artist)
                    songCallback(songs)
                    albumCallback(albums)
                }
            }
        }
    }

    var _artist: Artist? = null
    val artist: Artist get() = _artist!!
}
