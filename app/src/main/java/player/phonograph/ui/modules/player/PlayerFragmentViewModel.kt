/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.modules.player

import player.phonograph.model.Song
import player.phonograph.model.ui.PanelAction
import player.phonograph.repo.loader.FavoriteTracks
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerFragmentViewModel : ViewModel() {

    private var _favoriteState: MutableStateFlow<Pair<Song?, Boolean>> = MutableStateFlow(null to false)
    val favoriteState get() = _favoriteState.asStateFlow()

    private var loadFavoriteStateJob: Job? = null
    fun updateFavoriteState(context: Context, song: Song?) {
        loadFavoriteStateJob?.cancel()
        if (song != null && song.id > 0) {
            loadFavoriteStateJob = viewModelScope.launch {
                _favoriteState.emit(song to FavoriteTracks.isFavorite(context, song))
            }
        }
    }

    fun refreshFavoriteState(context: Context) {
        val song = favoriteState.value.first ?: return
        updateFavoriteState(context, song)
    }

    private var _shownToolbar: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val showToolbar get() = _shownToolbar.asStateFlow()

    fun toggleToolbar() =
        _shownToolbar.tryEmit(
            !_shownToolbar.value
        )

    private val _queuePanelEffects = MutableSharedFlow<PanelAction>()
    val queuePanelEffects get() = _queuePanelEffects.asSharedFlow()

    suspend fun updateQueuePanelState(action: PanelAction) {
        _queuePanelEffects.emit(action)
    }

    suspend fun collapsePanel() {
        updateQueuePanelState(PanelAction.Collapse)
    }

    suspend fun expandPanel() {
        updateQueuePanelState(PanelAction.Expand)
    }

    suspend fun togglePanel() {
        updateQueuePanelState(PanelAction.Toggle)
    }

}
