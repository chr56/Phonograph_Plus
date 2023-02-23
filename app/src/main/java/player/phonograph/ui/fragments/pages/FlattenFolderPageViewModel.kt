/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages

import player.phonograph.mediastore.SongCollectionLoader
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
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

    val sortMode: MutableStateFlow<SortMode> =
        MutableStateFlow(SortMode(SortRef.DISPLAY_NAME))


    fun loadFolders(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _folders.emit(
                SongCollectionLoader.getAllSongCollection(context = context).toMutableList().sort()
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

    private fun MutableList<SongCollection>.sort(): List<SongCollection> {
        sortBy {
            when (sortMode.value.sortRef) {
                SortRef.DISPLAY_NAME -> it.name
                else                 -> null
            }
        }
        if (sortMode.value.revert) reverse()
        return this
    }
}