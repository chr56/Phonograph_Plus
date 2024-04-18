/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.modules.playlist

import player.phonograph.mechanism.playlist.PlaylistEdit
import player.phonograph.model.Song
import player.phonograph.model.UIMode
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.GeneratedPlaylist
import player.phonograph.model.playlist.Playlist
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("LocalVariableName")
class PlaylistDetailViewModel(_playlist: Playlist) : ViewModel() {


    private val _playlist: MutableStateFlow<Playlist> = MutableStateFlow(_playlist)
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

    private val _searchResults: MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val searchResults get() = _searchResults.asStateFlow()

    fun searchSongs(keyword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = _songs.value.filter { it.title.contains(keyword) }
            _searchResults.emit(result)
        }
    }

    private val _keyword: MutableStateFlow<String> = MutableStateFlow("")
    val keyword get() = _keyword.asStateFlow()

    fun updateKeyword(string: String) {
        _keyword.value = string
    }

    private val _currentMode: MutableStateFlow<UIMode> = MutableStateFlow(UIMode.Common)
    val currentMode get() = _currentMode.asStateFlow()

    fun updateCurrentMode(newMode: UIMode) {
        _currentMode.value = newMode
    }

    fun moveItem(context: Context, fromPosition: Int, toPosition: Int): Deferred<Boolean> =
        viewModelScope.async(Dispatchers.IO) {
            if (fromPosition != toPosition) {
                PlaylistEdit.moveItem(context, playlist.value as FilePlaylist, fromPosition, toPosition)
            } else {
                false
            }
        }

    fun deleteItem(context: Context, songId: Long, index: Int): Deferred<Boolean> =
        viewModelScope.async(Dispatchers.IO) {
            PlaylistEdit.removeItem(context, playlist.value as FilePlaylist, songId, index.toLong()) > 0
        }

}