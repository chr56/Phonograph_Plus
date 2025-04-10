package player.phonograph.model.ui

import player.phonograph.R
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

enum class NowPlayingScreen(
    @field:StringRes @param:StringRes val titleRes: Int,
    @field:DrawableRes @param:DrawableRes val drawableResId: Int,
    val id: Int
) {
    CARD(R.string.card, R.drawable.player_card, 0),
    FLAT(R.string.flat, R.drawable.player_flat, 1);
}
