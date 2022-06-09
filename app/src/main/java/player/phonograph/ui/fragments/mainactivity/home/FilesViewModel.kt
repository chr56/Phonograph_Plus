/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import player.phonograph.App
import player.phonograph.mediastore.MediaStoreUtil
import player.phonograph.model.FileEntity
import player.phonograph.model.Location

class FilesViewModel : ViewModel() {
    var currentLocation: Location = Location.HOME

    var currentFileList: MutableList<FileEntity> = ArrayList<FileEntity>(1)
        private set

    private var listFileJob: Job? = null
    fun loadFiles(
        location: Location = currentLocation,
        context: Context = App.instance,
        onFinished: () -> Unit,
    ) {
        listFileJob?.cancel() // cancel current
        listFileJob =
            viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
                listFiles(location, context, this)
                withContext(Dispatchers.Main) { onFinished() }
            }
    }

    @Synchronized
    private fun listFiles(
        location: Location,
        context: Context = App.instance,
        scope: CoroutineScope?,
    ) {
        val paths = MediaStoreUtil.searchSongFiles(context, location.absolutePath) ?: return
        val list: MutableList<FileEntity> = ArrayList<FileEntity>(0)
        for (path in paths) {
            if (scope != null && !scope.isActive) return
            list.add(
                parsePath(currentLocation, path)
            )
        }
        currentFileList.clear()
        currentFileList.addAll(list)
    }

    companion object {
        fun parsePath(currentLocation: Location, absolutePath: String): FileEntity {
            val currentRelativePath = absolutePath.substringAfter(currentLocation.absolutePath).removePrefix("/")
            return if (currentRelativePath.contains('/')) {
                // folder
                FileEntity.Folder(
                    Location("${currentLocation.basePath}/${currentRelativePath.substringBefore('/')}", currentLocation.storageVolume)
                )
            } else {
                // file
                FileEntity.File(
                    Location("${currentLocation.basePath}/$currentRelativePath", currentLocation.storageVolume)
                )
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}
