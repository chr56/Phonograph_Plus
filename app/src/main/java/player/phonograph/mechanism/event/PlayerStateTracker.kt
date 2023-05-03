/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.event

import player.phonograph.service.player.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Suppress("ObjectPropertyName")
object PlayerStateTracker {
    private const val TAG = "PlayerStateTracker"

    private val _state = MutableStateFlow(PlayerState.PREPARING)
    val state get() = _state.asStateFlow()

    fun refreshState(newState: PlayerState) {
        _state.update { newState }
    }

    /*
    private val _current = MutableStateFlow(SoftReference(Song.EMPTY_SONG))
    val current get() = _state.asStateFlow()

    fun refreshCurrent(song: Song) {
        _current.update { SoftReference(song) }
    }
     */

}