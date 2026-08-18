/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.panel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _useTransparentStatusbar: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val useTransparentStatusbar get() = _useTransparentStatusbar.asStateFlow()

    fun updateStatusbarTransparent(transparent: Boolean) {
        _useTransparentStatusbar.value = transparent
    }

    private val _isMiniPlayerHidden: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isMiniPlayerHidden get() = _isMiniPlayerHidden.asStateFlow()

    fun updateMiniPlayerVisibility(hidden: Boolean) {
        _isMiniPlayerHidden.value = hidden
    }

    private val _effects = MutableSharedFlow<Action>()
    val effects get() = _effects.asSharedFlow()

    suspend fun updatePanel(action: Action) {
        _effects.emit(action)
    }

    sealed interface Action {
        object Expand : Action
        object Collapse : Action
        object Toggle : Action
    }

    suspend fun requestToCollapse() {
        updatePanel(Action.Collapse)
    }

    suspend fun requestToExpand() {
        updatePanel(Action.Expand)
    }

    suspend fun requestToToggle() {
        updatePanel(Action.Toggle)
    }

}