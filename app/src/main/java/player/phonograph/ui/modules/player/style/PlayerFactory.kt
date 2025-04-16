/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.player.style

import player.phonograph.model.ui.NowPlayingScreenStyle
import player.phonograph.model.ui.PlayerBaseStyle
import player.phonograph.ui.modules.player.AbsPlayerFragment
import android.os.Bundle

fun buildPlayerFragment(style: NowPlayingScreenStyle): AbsPlayerFragment {
    val fragment = when (style.baseStyle) {
        PlayerBaseStyle.FLAT -> FlatPlayerFragment()
        PlayerBaseStyle.CARD -> CardPlayerFragment()
    }
    return fragment.apply {
        arguments = Bundle().apply {
            putParcelable(AbsPlayerFragment.ARGUMENT_STYLE, style)
        }
    }
}