/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.modules.playlist2

import player.phonograph.mechanism.playlist2.EditablePlaylistProcessor
import player.phonograph.mechanism.playlist2.PlaylistProcessors
import player.phonograph.model.Song
import player.phonograph.model.UIMode
import player.phonograph.model.playlist2.Playlist
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
        viewModelScope.launch(Dispatchers.IO) {
            PlaylistProcessors.of(playlist).refresh(context)
            fetchAllSongs(context)
        }
    }

    private val _songs: MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val songs get() = _songs.asStateFlow()


    suspend fun fetchAllSongs(context: Context) {
        val playlistSongs = PlaylistProcessors.of(playlist.value).allSongs(context)
        _songs.emit(playlistSongs)
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
            val processor = PlaylistProcessors.of(playlist.value)
            if (processor is EditablePlaylistProcessor && fromPosition != toPosition) {
                processor.moveSong(context, fromPosition, toPosition)
            } else {
                false
            }
        }

    fun deleteItem(context: Context, song: Song, index: Int): Deferred<Boolean> =
        viewModelScope.async(Dispatchers.IO) {
            val processor = PlaylistProcessors.of(playlist.value)
            if (processor is EditablePlaylistProcessor) {
                processor.removeSong(context, song, index.toLong())
            } else {
                false
            }
        }

}