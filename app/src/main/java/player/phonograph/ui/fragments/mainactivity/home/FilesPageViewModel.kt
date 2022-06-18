/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlinx.coroutines.*
import player.phonograph.App
import player.phonograph.mediastore.MediaStoreUtil.searchSongFiles
import player.phonograph.model.FileEntity
import player.phonograph.model.Location
import player.phonograph.model.put
import player.phonograph.ui.fragments.mainactivity.folders.FileScanner

class FilesPageViewModel : ViewModel() {

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
                if (useMediaStore)
                    listFilesMediaStore(location, context, this)
                else
                    listFilesLegacy(location, context, this)
                withContext(Dispatchers.Main) { onFinished() }
            }
    }

    val useMediaStore = true // TODO

    @Synchronized
    private fun listFilesMediaStore(
        location: Location,
        context: Context,
        scope: CoroutineScope?,
    ) {
        currentFileList.clear()
        val set = searchSongFiles(context, location, scope) ?: return
        currentFileList.addAll(set)
    }

    @Synchronized
    private fun listFilesLegacy(
        location: Location,
        context: Context,
        scope: CoroutineScope?,
    ) {
        currentFileList.clear()
        // todo
        val directory = File(location.absolutePath).also { if (!it.isDirectory) return }
        val files = directory.listFiles(FileScanner.audioFileFilter) ?: return
        val list: MutableList<FileEntity> = ArrayList()
        for (file in files) {
            val l = Location.fromAbsolutePath(file.absolutePath)
            if (scope?.isActive == false) break
            val item =
                when {
                    file.isDirectory -> {
                        FileEntity.Folder(
                            location = l,
                            name = file.name,
                            dateAdded = file.lastModified(),
                            dateModified = file.lastModified()
                        )
                    }
                    file.isFile -> {
                        FileEntity.File(
                            location = l,
                            name = file.name,
                            size = file.length(),
                            dateAdded = file.lastModified(),
                            dateModified = file.lastModified()
                        )
                    }
                    else -> null
                }
            item?.let { list.put(it) }
        }
        currentFileList.addAll(list)
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}
