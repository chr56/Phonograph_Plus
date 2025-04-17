/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
enum class PlayerBaseStyle : Parcelable {
    @SerialName("CARD")
    CARD,
    @SerialName("FLAT")
    FLAT,
    ;
}