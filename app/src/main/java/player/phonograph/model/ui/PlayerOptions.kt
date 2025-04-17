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
data class PlayerOptions(
    @SerialName("mode_buttons_for_queue") val showModeButtonsForQueue: Boolean,
) : Parcelable