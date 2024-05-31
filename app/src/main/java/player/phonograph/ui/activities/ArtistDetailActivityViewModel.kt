/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import player.phonograph.App
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.repo.loader.Albums
import player.phonograph.repo.loader.Artists
import player.phonograph.repo.loader.Songs
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
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

class ArtistDetailActivityViewModel(var artistId: Long) : ViewModel() {

    private val _artist: MutableStateFlow<Artist?> = MutableStateFlow(null)
    val artist get() = _artist.asStateFlow()

    private val _albums: MutableStateFlow<List<Album>?> = MutableStateFlow(null)
    val albums get() = _albums.asStateFlow()

    private val _songs: MutableStateFlow<List<Song>?> = MutableStateFlow(null)
    val songs get() = _songs.asStateFlow()

    private val _paletteColor: MutableStateFlow<Int> = MutableStateFlow(0)
    val paletteColor get() = _paletteColor.asStateFlow()

    fun load(context: Context) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            _artist.emit(Artists.id(context, artistId))
            _albums.emit(Albums.artist(context, artistId))
            _songs.emit(Songs.artist(context, artistId))
        }
    }

    fun loadArtistImage(context: Context, artist: Artist, imageView: ImageView) {
        val defaultColor = ThemeSetting.primaryColor(context)
        loadImage(context)
            .from(artist)
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

    private var _usePaletteColor: MutableStateFlow<Boolean> =
        MutableStateFlow(Setting(App.instance)[Keys.albumArtistColoredFooters].data)
    val usePaletteColor = _usePaletteColor.asStateFlow()
    fun updateUsePaletteColor(newValue: Boolean) {
        _usePaletteColor.value = newValue
    }
}
