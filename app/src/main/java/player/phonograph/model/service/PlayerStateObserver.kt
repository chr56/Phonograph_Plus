/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.service

interface PlayerStateObserver {
    fun onPlayerStateChanged(oldState: PlayerState, newState: PlayerState) {}
    fun onReceivingMessage(msg: Int)


    companion object{
        const val MSG_NOW_PLAYING_CHANGED = 8
        const val MSG_NO_MORE_SONGS = 16
        const val MSG_PLAYER_STOPPED = 128
    }

}