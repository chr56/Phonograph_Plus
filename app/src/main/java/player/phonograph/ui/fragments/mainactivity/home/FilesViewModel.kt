/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.*
import kotlinx.coroutines.*
import player.phonograph.App
import player.phonograph.mediastore.MediaStoreUtil.searchSongFiles
import player.phonograph.model.FileEntity
import player.phonograph.model.Location

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
        currentFileList.clear()
        val set = searchSongFiles(context, location, scope) ?: return
        currentFileList.addAll(set)
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}
