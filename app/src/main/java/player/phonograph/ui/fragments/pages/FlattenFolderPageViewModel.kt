/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages

import player.phonograph.mediastore.SongCollectionLoader
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FlattenFolderPageViewModel : ViewModel() {

    /**
     * true if browsing folders
     */
    val mainViewMode: MutableStateFlow<Boolean> = MutableStateFlow(true)

    private val _folders: MutableStateFlow<List<SongCollection>> = MutableStateFlow(emptyList())
    val folders = _folders.asStateFlow()

    private val _currentSongs: MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val currentSongs = _currentSongs.asStateFlow()


    fun loadFolders(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _folders.emit(
                SongCollectionLoader.getAllSongCollection(context = context)
            )
        }
    }

    fun browseFolder(position: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = _folders.value[position].songs
            _currentSongs.emit(songs)
            mainViewMode.emit(false)
        }
    }

}