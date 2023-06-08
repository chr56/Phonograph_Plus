/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.model.PlaylistDetailMode
import player.phonograph.model.Song
import player.phonograph.model.playlist.GeneratedPlaylist
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


    fun refreshPlaylist(context: Context) {
        val playlist = _playlist.value
        if (playlist is GeneratedPlaylist) {
            playlist.refresh(context)
        }
        fetchAllSongs(context)
    }


    private val _songs: MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val songs get() = _songs.asStateFlow()


    fun fetchAllSongs(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _songs.emit(playlist.value.getSongs(context))
        }
    }

    fun searchSongs(context: Context, keyword: String) { // todo better implement
        viewModelScope.launch(Dispatchers.IO) {
            val allSongs = playlist.value.getSongs(context)
            val result = allSongs.filter { it.title.contains(keyword) }
            _songs.emit(result)
        }
    }

    private val _keyword: MutableStateFlow<String> = MutableStateFlow("")
    val keyword get() = _keyword.asStateFlow()

    fun updateKeyword(string: String) {
        _keyword.value = string
    }

    private val _currentMode: MutableStateFlow<PlaylistDetailMode> = MutableStateFlow(PlaylistDetailMode.Common)
    val currentMode get() = _currentMode.asStateFlow()

    var previousMode: PlaylistDetailMode = PlaylistDetailMode.Common
        private set

    fun updateCurrentMode(newMode: PlaylistDetailMode) {
        previousMode = _currentMode.value
        _currentMode.value = newMode
    }
}