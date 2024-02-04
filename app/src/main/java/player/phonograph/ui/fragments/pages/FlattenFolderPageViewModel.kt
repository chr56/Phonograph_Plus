/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.pages

import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.loaders.SongCollectionLoader
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
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
    private val _mainViewMode: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val mainViewMode = _mainViewMode.asStateFlow()

    private val _folders: MutableStateFlow<List<SongCollection>> = MutableStateFlow(emptyList())
    val folders = _folders.asStateFlow()

    private val _currentSongs: MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val currentSongs = _currentSongs.asStateFlow()

    private var _currentPosition: Int = -1
    val currentFolder get() = _folders.value.getOrNull(_currentPosition)

    private var _bannerText: MutableStateFlow<String> = MutableStateFlow("")
    val bannerText = _bannerText.asStateFlow()


    fun updateBannerText(context: Context, mode: Boolean) {
        _bannerText.tryEmit(
            if (mode) {
                context.resources.getQuantityString(
                    R.plurals.item_files, folders.value.size, folders.value.size
                )
            } else {
                context.resources.getQuantityString(
                    R.plurals.item_songs, currentSongs.value.size, currentSongs.value.size
                )
            }
        )
    }

    /**
     * update folders
     */
    fun loadFolders(context: Context, updateBanner: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _folders.emit(
                SongCollectionLoader.all(context = context).toMutableList().sort(context)
            )
            if (updateBanner) updateBannerText(context, true)
        }
    }

    /**
     * browse folder at [position]
     */
    fun browseFolder(context: Context, position: Int, updateBanner: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentPosition = position
            loadSongs(context, updateBanner)
            _mainViewMode.emit(false)
        }
    }

    /**
     * update Songs
     */
    fun loadSongs(context: Context, updateBanner: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentSongs.emit(currentFolder?.songs ?: emptyList())
            if (updateBanner) updateBannerText(context, false)
        }
    }

    /**
     * try to get back to upper level
     * @return reached top
     */
    fun navigateUp(context: Context): Boolean =
        if (mainViewMode.value) {
            true
        } else {
            _mainViewMode.value = true
            _currentPosition = -1
            _currentSongs.tryEmit(emptyList())
            updateBannerText(context, true)
            false
        }

    private fun MutableList<SongCollection>.sort(context: Context): List<SongCollection> {
        val mode = Setting(context).Composites[Keys.collectionSortMode].data
        sortBy {
            when (mode.sortRef) {
                SortRef.DISPLAY_NAME -> it.name
                else                 -> null
            }
        }
        if (mode.revert) reverse()
        return this
    }
}