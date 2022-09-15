/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.preferences

import player.phonograph.model.NowPlayingScreen
import player.phonograph.settings.Setting

object NowPlayingScreenConfig {
    var nowPlayingScreen: NowPlayingScreen
        get() {
            val id = Setting.instance.nowPlayingScreenIndex
            for (nowPlayingScreen in NowPlayingScreen.values()) {
                if (nowPlayingScreen.id == id) return nowPlayingScreen
            }
            return NowPlayingScreen.CARD
        }
        set(nowPlayingScreen) {
            Setting.instance.nowPlayingScreenIndex = nowPlayingScreen.id
        }
}