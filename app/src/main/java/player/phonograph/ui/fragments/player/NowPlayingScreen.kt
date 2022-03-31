package player.phonograph.ui.fragments.player

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import player.phonograph.R

enum class NowPlayingScreen(
    @field:StringRes @param:StringRes val titleRes: Int,
    @field:DrawableRes @param:DrawableRes val drawableResId: Int,
    val id: Int
) {
    CARD(R.string.card, R.drawable.np_card, 0),
    FLAT(R.string.flat, R.drawable.np_flat, 1);
}
