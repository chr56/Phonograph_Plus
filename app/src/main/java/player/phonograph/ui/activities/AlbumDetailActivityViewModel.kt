/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.AlbumLoader
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class AlbumDetailActivityViewModel(val albumId: Long) : ViewModel() {

    var isRecyclerViewPrepared: Boolean = false

    private var _album: Album? = null
    val album: Album get() = _album ?: Album()

    fun loadDataSet(
        context: Context,
        callback: (Album, List<Song>) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {

            _album = AlbumLoader.id(context, albumId)

            val songs: List<Song> = album.songs

            while (!isRecyclerViewPrepared) yield() // wait until ready
            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) {
                    callback(album, songs)
                }
            }
        }
    }

    val paletteColor: MutableStateFlow<Int> = MutableStateFlow(0)
}
