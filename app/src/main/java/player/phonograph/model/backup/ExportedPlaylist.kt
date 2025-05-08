/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.backup

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
class ExportedPlaylist(
    @SerialName("path") val path: String,
    @SerialName("title") val name: String,
)

