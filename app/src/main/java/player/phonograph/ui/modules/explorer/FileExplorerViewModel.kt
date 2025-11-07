/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.modules.explorer

import lib.storage.extension.rootDirectory
import player.phonograph.App
import player.phonograph.mechanism.explorer.MediaPaths
import player.phonograph.model.file.FileItem
import player.phonograph.model.file.MediaPath
import player.phonograph.repo.mediastore.MediaStoreFileEntities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FileExplorerViewModel : ViewModel() {

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _currentPath: MutableStateFlow<MediaPath> =
        MutableStateFlow(MediaPaths.startDirectory(App.instance))
    val currentPath get() = _currentPath.asStateFlow()

    // adapter position history
    private val history: MutableMap<MediaPath, Int> = mutableMapOf()
    val historyPosition: Int get() = history[_currentPath.value] ?: 0
    fun changeDirectory(context: Context, position: Int, newLocation: MediaPath) {
        val oldLocation = _currentPath.value
        history[oldLocation] = position
        _currentPath.value = newLocation
        refreshFiles(context, newLocation)
    }

    private val _currentFiles: MutableStateFlow<List<FileItem>> = MutableStateFlow(emptyList())
    val currentFiles = _currentFiles.asStateFlow()

    fun refreshFiles(context: Context, path: MediaPath = currentPath.value) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true

            _currentFiles.value = if (optionUseLegacyListFile) {
                MediaStoreFileEntities.listFilesLegacy(context, path)
            } else {
                MediaStoreFileEntities.listFilesMediaStore(context, path)
            }
            _loading.value = false
        }
    }

    fun volumes(context: Context): VolumeOverview {
        val volumes = MediaPaths.volumes(context)
        val volumesRoots =
            volumes.map { it.rootDirectory() }.map { if (it != null) MediaPaths.from(it, context) else null }
        val volumesNames =
            volumes.map { "${it.getDescription(context)}\n(${it.rootDirectory()?.path ?: "N/A"})" }
        val currentVolume =
            volumes.find { it.uuid.orEmpty() == currentPath.value.volume.uuid }
        return VolumeOverview(volumesNames, volumesRoots, volumes.indexOf(currentVolume))
    }

    var optionUseLegacyListFile: Boolean = false

    class VolumeOverview(
        val names: List<String>,
        val paths: List<MediaPath?>,
        val current: Int,
    )
}
