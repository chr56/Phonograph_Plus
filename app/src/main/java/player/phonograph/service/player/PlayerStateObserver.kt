/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.player

interface PlayerStateObserver {
    fun onPlayerStateChanged(oldState: PlayerState, newState: PlayerState) {}
    fun onReceivingMessage(msg: Int)
}

inline fun MutableList<PlayerStateObserver>.executeForEach(
    action: PlayerStateObserver.() -> Unit
) {
    for (observer in this) {
        action(observer)
    }
}
