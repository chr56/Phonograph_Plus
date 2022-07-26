/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.util.*
import kotlinx.coroutines.*
import player.phonograph.App
import player.phonograph.mediastore.MediaStoreUtil.searchSongFiles
import player.phonograph.model.FileEntity
import player.phonograph.model.Location
import player.phonograph.settings.Setting
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
                if (useLegacyListFile)
                    listFilesLegacy(location, context, this)
                else
                    listFilesMediaStore(location, context, this)
                withContext(Dispatchers.Main) { onFinished() }
            }
    }

    var useLegacyListFile: Boolean
        get() = Setting.instance.useLegacyListFilesImpl
        set(value) { Setting.instance.useLegacyListFilesImpl = value }

    var showFilesImages: Boolean
        get() = Setting.instance.showFileImages
        set(value) { Setting.instance.showFileImages = value }

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
