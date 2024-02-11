/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.App
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainDrawerViewModel : ViewModel() {

    private val _selectedPage: MutableStateFlow<Int> = MutableStateFlow(0)
    val selectedPage: StateFlow<Int> = _selectedPage.asStateFlow()

    fun switchPageTo(page: Int) {
        _selectedPage.value = page
        viewModelScope.launch(Dispatchers.IO) {
            Setting(App.instance)[Keys.lastPage].edit { page }
        }
    }
}