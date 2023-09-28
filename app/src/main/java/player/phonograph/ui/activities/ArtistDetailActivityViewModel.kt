/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.ArtistAlbumLoader.allAlbums
import player.phonograph.repo.mediastore.loaders.ArtistLoader
import player.phonograph.repo.mediastore.loaders.ArtistSongLoader.allSongs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.widget.ImageView
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
        viewModelScope.launch(SupervisorJob()) {
            val artist = ArtistLoader.id(context, artistId)
            _artist.emit(artist)
            _albums.emit(artist.allAlbums(context))
            _songs.emit(artist.allSongs(context))
        }
    }

    fun loadArtistImage(context: Context, artist: Artist, imageView: ImageView) {
        val defaultColor = ThemeColor.primaryColor(context)
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

}
