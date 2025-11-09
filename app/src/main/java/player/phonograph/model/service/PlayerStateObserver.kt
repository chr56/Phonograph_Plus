/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.service

interface PlayerStateObserver {
    fun onPlayerStateChanged(oldState: PlayerState, newState: PlayerState) {}
}