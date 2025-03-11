/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.App
import player.phonograph.mechanism.explorer.Locations
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AbsFileViewModel : ViewModel() {


    private val _currentLocation: MutableStateFlow<Location> =
        MutableStateFlow(Locations.from(Setting(App.instance).Composites[Keys.startDirectory].data))

    val currentLocation = _currentLocation.asStateFlow()


    // adapter position history
    private val history: MutableMap<Location, Int> = mutableMapOf()
    val historyPosition: Int get() = history[_currentLocation.value] ?: 0
    fun changeLocation(context: Context, position: Int, newLocation: Location) {
        val oldLocation = _currentLocation.value
        history[oldLocation] = position
        _currentLocation.value = newLocation
        refreshFiles(context, newLocation)
    }

    fun refreshFiles(context: Context, location: Location = currentLocation.value) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true

            _currentFiles.value = listFiles(context, location, this)

            _loading.value = false
        }
    }

    private val _currentFiles: MutableStateFlow<List<FileEntity>> = MutableStateFlow(emptyList())
    val currentFiles = _currentFiles.asStateFlow()

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    override fun onCleared() {
        viewModelScope.cancel()
    }

    protected abstract suspend fun listFiles(
        context: Context,
        location: Location,
        scope: CoroutineScope?,
    ): List<FileEntity>
}
