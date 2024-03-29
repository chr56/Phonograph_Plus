/*
 * Copyright (c) 2022-2023 chr_56
 */

package player.phonograph.mechanism.setting

import player.phonograph.App
import player.phonograph.model.NowPlayingScreen
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting

object NowPlayingScreenConfig {
    var nowPlayingScreen: NowPlayingScreen
        get() {
            val id = Setting(App.instance)[Keys.nowPlayingScreenIndex].data
            for (nowPlayingScreen in NowPlayingScreen.values()) {
                if (nowPlayingScreen.id == id) return nowPlayingScreen
            }
            return NowPlayingScreen.CARD
        }
        set(nowPlayingScreen) {
            Setting(App.instance)[Keys.nowPlayingScreenIndex].data = nowPlayingScreen.id
        }
}