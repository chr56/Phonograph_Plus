/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.explorer

import player.phonograph.mechanism.setting.FileConfig
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.TreeSet

abstract class AbsFileViewModel : ViewModel() {


    private val _currentLocation: MutableStateFlow<Location> =
        MutableStateFlow(Location.from(FileConfig.startDirectory))
    val currentLocation get() = _currentLocation.asStateFlow()

    fun changeLocation(context: Context, newLocation: Location) {
        _currentLocation.value = newLocation
        refreshFiles(context, newLocation)
    }

    fun refreshFiles(context: Context, location: Location = currentLocation.value) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true

            val files = listFiles(context, location, this)
            if (!isActive) return@launch
            _currentFiles.value = files

            _loading.value = false
        }
    }

    private val _currentFiles: MutableStateFlow<Set<FileEntity>> =
        MutableStateFlow(TreeSet<FileEntity>())
    val currentFiles get() = _currentFiles.asStateFlow()

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading get() = _loading.asStateFlow()

    override fun onCleared() {
        viewModelScope.cancel()
    }

    protected abstract suspend fun listFiles(
        context: Context,
        location: Location,
        scope: CoroutineScope?,
    ): Set<FileEntity>
}
