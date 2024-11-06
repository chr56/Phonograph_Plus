/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.panel

import player.phonograph.ui.fragments.player.AbsPlayerFragment
import player.phonograph.ui.fragments.player.MiniPlayerFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class PanelViewModel(
    initialActivityColor: Int,
    initialHighlightColor: Int,
    val defaultColor: Int,
) : ViewModel() {

    // original color of this activity
    private val _activityColor: MutableStateFlow<Int> = MutableStateFlow(initialActivityColor)
    val activityColor get() = _activityColor.asStateFlow()

    fun updateActivityColor(newColor: Int) {
        viewModelScope.launch { _activityColor.emit(newColor) }
    }

    private val _highlightColor: MutableStateFlow<Int> = MutableStateFlow(initialHighlightColor)
    val highlightColor get() = _highlightColor.asStateFlow()

    private val _previousHighlightColor: MutableStateFlow<Int> = MutableStateFlow(initialHighlightColor)
    val previousHighlightColor get() = _previousHighlightColor.asStateFlow()

    fun updateHighlightColor(newColor: Int) {
        viewModelScope.launch {
            val oldColor = _highlightColor.value
            _previousHighlightColor.emit(oldColor)
            _highlightColor.emit(newColor)
        }
    }

    val playerFragment: WeakReference<AbsPlayerFragment?> = WeakReference(null)
    val miniPlayerFragment: WeakReference<MiniPlayerFragment?> = WeakReference(null)


}