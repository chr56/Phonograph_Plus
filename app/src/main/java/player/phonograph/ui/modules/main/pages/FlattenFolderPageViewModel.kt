/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.main.pages

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
import android.util.SparseIntArray
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

    private var _currentFolderIndex: Int = -1
    val currentFolder get() = _folders.value.getOrNull(_currentFolderIndex)

    private var _bannerText: MutableStateFlow<String> = MutableStateFlow("")
    val bannerText = _bannerText.asStateFlow()

    var historyFolderPosition: Int = 0
        private set
    private val historyPositions: SparseIntArray = SparseIntArray()
    val historyPosition: Int get() = historyPositions[_currentFolderIndex]

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
     * browse folder at [folderIndex]
     */
    fun browseFolder(context: Context, folderIndex: Int, updateBanner: Boolean, lastScrollPosition: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            historyFolderPosition = lastScrollPosition
            _currentFolderIndex = folderIndex
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
    fun navigateUp(context: Context, lastScrollPosition: Int): Boolean =
        if (mainViewMode.value) {
            true
        } else {
            _mainViewMode.value = true
            historyPositions.put(_currentFolderIndex, lastScrollPosition)
            _currentFolderIndex = -1
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