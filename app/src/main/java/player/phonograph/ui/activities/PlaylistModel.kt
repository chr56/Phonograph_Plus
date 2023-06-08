/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistModel : ViewModel() {


    fun initPlaylist(playlist: Playlist) {
        _playlist = MutableStateFlow(playlist)
    }

    private lateinit var _playlist: MutableStateFlow<Playlist>
    val playlist get() = _playlist.asStateFlow()


    private val _songs: MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val songs get() = _songs.asStateFlow()


    fun fetchSongs(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _songs.emit(playlist.value.getSongs(context))
        }
    }

    var editMode: Boolean = false
}