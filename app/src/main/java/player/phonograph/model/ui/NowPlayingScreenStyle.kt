/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class NowPlayingScreenStyle(
    @SerialName("base_style") val baseStyle: PlayerBaseStyle,
    @SerialName("controller_style") val controllerStyle: PlayerControllerStyle,
    @SerialName("options") val options: PlayerOptions,
) : Parcelable {
    companion object {
        val DEFAULT: NowPlayingScreenStyle
            get() = NowPlayingScreenStyle(
                baseStyle = PlayerBaseStyle.CARD,
                controllerStyle = PlayerControllerStyle.DEFAULT,
                options = PlayerOptions(
                    showModeButtonsForQueue = true
                )
            )
    }
}