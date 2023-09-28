/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.AlbumLoader
import player.phonograph.repo.mediastore.loaders.AlbumSongLoader
import player.phonograph.repo.mediastore.loaders.AlbumSongLoader.allSongs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumDetailActivityViewModel(val albumId: Long) : ViewModel() {

    var isRecyclerViewPrepared: Boolean = false

    private var _album: MutableStateFlow<Album> = MutableStateFlow(Album())
    val album get() = _album.asStateFlow()

    private var _songs:MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val songs get() = _songs.asStateFlow()

    fun loadDataSet(context: Context) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {

            val album = AlbumLoader.id(context, albumId)
            val songs: List<Song> = album.allSongs(context)
            _album.emit(album)
            _songs.emit(songs)
        }
    }

    private val _paletteColor: MutableStateFlow<Int> = MutableStateFlow(0)
    val paletteColor get() = _paletteColor.asStateFlow()

    fun loadAlbumImage(context: Context, album: Album, imageView: ImageView) {
        val defaultColor = ThemeColor.primaryColor(context)
        loadImage(context)
            .from(AlbumSongLoader.id(context, album.id).firstOrNull())
            .into(
                PaletteTargetBuilder(defaultColor)
                    .onResourceReady { result, color ->
                        imageView.setImageDrawable(result)
                        _paletteColor.tryEmit(color)
                    }
                    .onFail {
                        imageView.setImageResource(R.drawable.default_album_art)
                        _paletteColor.tryEmit(defaultColor)
                    }
                    .build()
            )
            .enqueue()
    }
}
