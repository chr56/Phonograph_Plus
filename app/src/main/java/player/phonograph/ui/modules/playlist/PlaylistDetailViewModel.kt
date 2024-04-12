/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.modules.playlist

import player.phonograph.model.Song
import player.phonograph.model.UIMode
import player.phonograph.model.playlist.GeneratedPlaylist
import player.phonograph.model.playlist.Playlist
import util.phonograph.playlist.mediastore.moveItemViaMediastore
import util.phonograph.playlist.mediastore.removeFromPlaylistViaMediastore
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

    private val _currentMode: MutableStateFlow<UIMode> = MutableStateFlow(UIMode.Common)
    val currentMode get() = _currentMode.asStateFlow()

    fun updateCurrentMode(context: Context, newMode: UIMode) {
        // todo
        when (newMode) {
            UIMode.Search -> searchSongs(context, keyword.value)
            UIMode.Common -> fetchAllSongs(context)
            UIMode.Editor -> fetchAllSongs(context)
        }
        _currentMode.value = newMode
    }

    fun moveItem(context: Context, fromPosition: Int, toPosition: Int): Deferred<Boolean> =
        viewModelScope.async(Dispatchers.IO) {
            if (fromPosition != toPosition) {
                moveItemViaMediastore(context, playlist.value.id, fromPosition, toPosition)
            } else {
                false
            }
        }

    fun deleteItem(context: Context, song: Song): Deferred<Boolean> =
        viewModelScope.async(Dispatchers.IO) {
            removeFromPlaylistViaMediastore(context, song, playlist.value.id)
        }

}