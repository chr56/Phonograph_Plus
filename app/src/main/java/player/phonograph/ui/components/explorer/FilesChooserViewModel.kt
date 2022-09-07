/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.explorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.util.FileUtil.FileScanner
import java.io.File
import java.util.*

class FilesChooserViewModel : ViewModel() {

    var currentLocation: Location = Location.HOME

    var currentFileList: MutableSet<FileEntity> = TreeSet<FileEntity>()
        private set

    private var listFileJob: Job? = null
    fun loadFiles(
        location: Location = currentLocation,
        onFinished: () -> Unit,
    ) {
        listFileJob?.cancel() // cancel current
        listFileJob =
            viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
                listFile(location, this)
                withContext(Dispatchers.Main) { onFinished() }
            }
    }


    @Synchronized
    private fun listFile(
        location: Location,
        scope: CoroutineScope?,
    ) {
        currentFileList.clear()
        // todo
        val directory = File(location.absolutePath).also { if (!it.isDirectory) return }
        val files = directory.listFiles() ?: return
        val set = TreeSet<FileEntity>()
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
                        ).also { it.songCount = 0 }
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
            item?.let { set.add(it) }
        }
        currentFileList.addAll(set)
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}
