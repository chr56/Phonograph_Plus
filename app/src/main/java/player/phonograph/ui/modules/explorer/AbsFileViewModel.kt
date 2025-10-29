/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.App
import player.phonograph.mechanism.explorer.MediaPaths
import player.phonograph.model.file.FileItem
import player.phonograph.model.file.MediaPath
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AbsFileViewModel : ViewModel() {

    val defaultPath: String get() = Setting(App.instance)[Keys.startDirectoryPath].data

    private val _currentPath: MutableStateFlow<MediaPath> =
        MutableStateFlow(MediaPaths.from(defaultPath, App.instance))

    val currentPath = _currentPath.asStateFlow()


    // adapter position history
    private val history: MutableMap<MediaPath, Int> = mutableMapOf()
    val historyPosition: Int get() = history[_currentPath.value] ?: 0
    fun changeDirectory(context: Context, position: Int, newLocation: MediaPath) {
        val oldLocation = _currentPath.value
        history[oldLocation] = position
        _currentPath.value = newLocation
        refreshFiles(context, newLocation)
    }

    fun refreshFiles(context: Context, path: MediaPath = currentPath.value) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true

            _currentFiles.value = listFiles(context, path)

            _loading.value = false
        }
    }

    private val _currentFiles: MutableStateFlow<List<FileItem>> = MutableStateFlow(emptyList())
    val currentFiles = _currentFiles.asStateFlow()

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    override fun onCleared() {
        viewModelScope.cancel()
    }

    protected abstract suspend fun listFiles(
        context: Context,
        path: MediaPath,
    ): List<FileItem>
}
