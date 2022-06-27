/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.activities

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist

class PlaylistModel : ViewModel() {

    var playlist: MutableLiveData<Playlist> = MutableLiveData()

    var isRecyclerViewReady = false

    fun fetchPlaylist(context: Context, callback: PlaylistCallback) {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = playlist.value?.getSongs(context) ?: emptyList()
            while (!isRecyclerViewReady) yield()
            withContext(Dispatchers.Main) {
                callback?.invoke(playlist.value!!, songs)
            }
        }
    }

}

typealias PlaylistCallback = ((playlist: Playlist, List<Song>) -> Unit)?
