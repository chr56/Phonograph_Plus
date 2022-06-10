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
import java.util.*

class FilesViewModel : ViewModel() {
    var currentLocation: Location = Location.HOME

    var currentFileList: MutableSet<FileEntity> = TreeSet<FileEntity>()
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
        val list: MutableSet<FileEntity> = TreeSet<FileEntity>()
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
            val basePath = currentLocation.basePath.let { if (it == "/") "" else it } // root //todo
            return if (currentRelativePath.contains('/')) {
                // folder
                FileEntity.Folder(
                    Location("$basePath/${currentRelativePath.substringBefore('/')}", currentLocation.storageVolume)
                )
            } else {
                // file
                FileEntity.File(
                    Location("$basePath/$currentRelativePath", currentLocation.storageVolume)
                )
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}
