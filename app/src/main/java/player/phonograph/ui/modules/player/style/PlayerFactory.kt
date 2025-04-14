/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.player.style

import player.phonograph.model.ui.NowPlayingScreen
import player.phonograph.ui.modules.player.AbsPlayerFragment

fun buildPlayerFragment(screen: NowPlayingScreen): AbsPlayerFragment {
    return when (screen) {
        NowPlayingScreen.FLAT -> FlatPlayerFragment()
        NowPlayingScreen.CARD -> CardPlayerFragment()
    }
}