/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import player.phonograph.mediastore.ArtistLoader
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ArtistDetailActivityViewModel(var artistId: Long) : ViewModel() {

    private var _artist: MutableStateFlow<Artist?> = MutableStateFlow(null)
    val artist get() = _artist.asStateFlow()

    private var _albums: MutableStateFlow<List<Album>?> = MutableStateFlow(null)
    val albums get() = _albums.asStateFlow()

    private var _songs: MutableStateFlow<List<Song>?> = MutableStateFlow(null)
    val songs get() = _songs.asStateFlow()


    fun load(context: Context) {
        viewModelScope.launch(SupervisorJob()) {
            val artist = ArtistLoader.getArtist(context, artistId)
            _artist.emit(artist)
            _albums.emit(artist.albums)
            _songs.emit(artist.songs)
        }
    }

}
