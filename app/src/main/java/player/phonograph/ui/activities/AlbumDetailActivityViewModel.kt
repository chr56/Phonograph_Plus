/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import player.phonograph.mediastore.AlbumLoader
import player.phonograph.model.Album
import player.phonograph.model.Song

class AlbumDetailActivityViewModel : ViewModel() {
    private val loaderCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    var isRecyclerViewPrepared: Boolean = false

    var albumId: Long = -1
    private var _album: Album? = null
    val album: Album get() = _album ?: Album()

    fun loadDataSet(
        context: Context,
        albumCallback: (Album) -> Unit,
        songCallback: (List<Song>) -> Unit
    ) {
        loaderCoroutineScope.launch {

            _album = AlbumLoader.getAlbum(context, albumId)

            val songs: List<Song> = album.songs

            while (!isRecyclerViewPrepared) yield() // wait until ready
            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) {
                    albumCallback(album)
                    songCallback(songs)
                }
            }
        }
    }

    var paletteColor: Int = 0
}
