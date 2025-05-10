/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.panel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PanelViewModel(
    initialActivityColor: Int,
    initialHighlightColor: Int,
) : ViewModel() {

    // original color of this activity
    private val _activityColor: MutableStateFlow<Int> = MutableStateFlow(initialActivityColor)
    val activityColor get() = _activityColor.asStateFlow()

    fun updateActivityColor(newColor: Int) {
        viewModelScope.launch { _activityColor.emit(newColor) }
    }

    private val _highlightColor: MutableStateFlow<Int> = MutableStateFlow(initialHighlightColor)
    val highlightColor get() = _highlightColor.asStateFlow()

    private val _colorChange = MutableStateFlow(initialHighlightColor to initialHighlightColor)
    val colorChange get() = _colorChange.asStateFlow()

    fun updateHighlightColor(newColor: Int) {
        viewModelScope.launch {
            val oldColor = _highlightColor.value
            _colorChange.emit(oldColor to newColor)
            _highlightColor.emit(newColor)
        }
    }

    private val _isPanelHidden: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isPanelHidden get() = _isPanelHidden.asStateFlow()

    fun updatePanelState(hidden: Boolean) {
        _isPanelHidden.value = hidden
    }

}