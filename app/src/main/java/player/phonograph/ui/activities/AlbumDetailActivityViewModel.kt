/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.repo.loader.Albums
import player.phonograph.repo.loader.Songs
import player.phonograph.settings.ThemeSetting
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

    private var _album: MutableStateFlow<Album> = MutableStateFlow(Album())
    val album get() = _album.asStateFlow()

    private var _songs:MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val songs get() = _songs.asStateFlow()

    fun loadDataSet(context: Context) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            _album.emit(Albums.id(context, albumId))
            _songs.emit(Songs.album(context, albumId).sortedBy { it.trackNumber })
        }
    }

    private val _paletteColor: MutableStateFlow<Int> = MutableStateFlow(0)
    val paletteColor get() = _paletteColor.asStateFlow()

    fun loadAlbumImage(context: Context, album: Album, imageView: ImageView) {
        val defaultColor = ThemeSetting.primaryColor(context)
        loadImage(context)
            .from(album)
            .into(
                PaletteTargetBuilder()
                    .defaultColor(defaultColor)
                    .onStart {
                        imageView.setImageResource(R.drawable.default_album_art)
                        _paletteColor.tryEmit(defaultColor)
                    }
                    .onResourceReady { result, color ->
                        imageView.setImageDrawable(result)
                        _paletteColor.tryEmit(color)
                    }
                    .build()
            )
            .enqueue()
    }
}
