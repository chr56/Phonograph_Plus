/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.explorer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.mechanism.setting.FileConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

abstract class AbsFileViewModel : ViewModel() {


    private val _currentLocation: MutableStateFlow<Location> =
        MutableStateFlow(Location.from(FileConfig.startDirectory))
    val currentLocation get() = _currentLocation.asStateFlow()

    fun changeLocation(newLocation: Location) {
        _currentLocation.value = newLocation
    }

    var currentFileList: MutableSet<FileEntity> = TreeSet<FileEntity>()

    override fun onCleared() {
        viewModelScope.cancel()
    }

    private var listFileJob: Job? = null
    fun loadFiles(
        context: Context,
        location: Location = currentLocation.value,
        onFinished: () -> Unit,
    ) {
        listFileJob?.cancel() // cancel current
        listFileJob =
            viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
                onLoadFiles(location, context, this)
                withContext(Dispatchers.Main) { onFinished() }
            }
    }

    protected abstract fun onLoadFiles(location: Location, context: Context, scope: CoroutineScope?)
}
