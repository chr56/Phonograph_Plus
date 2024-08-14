/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.modules.playlist

import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.Song
import player.phonograph.model.UIMode
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
import kotlinx.coroutines.withContext

@Suppress("LocalVariableName")
class PlaylistDetailViewModel(_playlist: Playlist) : ViewModel() {

    val playlist: Playlist = _playlist

    private val _songs: MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val songs get() = _songs.asStateFlow()

    suspend fun fetchAllSongs(context: Context) {
        withContext(Dispatchers.IO) {
            val playlistSongs = PlaylistProcessors.reader(playlist).allSongs(context)
            _songs.emit(playlistSongs)
        }
    }

    fun refreshPlaylist(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            PlaylistProcessors.reader(playlist).refresh(context)
        }
    }

    private val _searchResults: MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val searchResults get() = _searchResults.asStateFlow()

    suspend fun searchSongs(keyword: String) {
        withContext(Dispatchers.IO) {
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
            PlaylistProcessors.writer(playlist)?.moveSong(context, fromPosition, toPosition) ?: false
        }

    fun deleteItem(context: Context, song: Song, index: Int): Deferred<Boolean> =
        viewModelScope.async(Dispatchers.IO) {
            PlaylistProcessors.writer(playlist)?.removeSong(context, song, index.toLong()) ?: false
        }

}