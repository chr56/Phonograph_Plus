/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.main

import player.phonograph.model.pages.PagesConfig
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.settings.SettingObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainDrawerViewModel : ViewModel() {

    private val _selectedPage: MutableStateFlow<Int> = MutableStateFlow(0)
    val selectedPage: StateFlow<Int> = _selectedPage.asStateFlow()

    fun switchPageTo(context: Context, page: Int) {
        _selectedPage.value = page
        viewModelScope.launch(Dispatchers.IO) {
            Setting(context)[Keys.lastPage].edit { page }
        }
    }

    private val _pages: MutableStateFlow<PagesConfig?> = MutableStateFlow(null)
    val pages: StateFlow<PagesConfig?> = _pages.asStateFlow()

    private val _rememberLastTab: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val rememberLastTab: StateFlow<Boolean> = _rememberLastTab.asStateFlow()

    private val _lastPage: MutableStateFlow<Int> = MutableStateFlow(-1)
    val lastPage: StateFlow<Int> = _lastPage.asStateFlow()

    private val _fixedTabLayout: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val fixedTabLayout: StateFlow<Boolean> = _fixedTabLayout.asStateFlow()


    fun observeSettings(context: Context) {
        SettingObserver(context, viewModelScope).apply {
            collect(Keys.homeTabConfig, Dispatchers.IO) {
                _pages.value = it
            }
            collect(Keys.rememberLastTab, Dispatchers.IO) {
                _rememberLastTab.value = it
            }
            collect(Keys.lastPage, Dispatchers.IO) {
                _lastPage.value = it
            }
            collect(Keys.fixedTabLayout, Dispatchers.IO) {
                _fixedTabLayout.value = it
            }
        }
    }
}