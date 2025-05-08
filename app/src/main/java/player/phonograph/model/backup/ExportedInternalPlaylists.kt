/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.backup

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
class ExportedInternalPlaylists(
    @SerialName("version") val version: Int,
    @SerialName(PLAYLISTS) val playlists: List<ExportedInternalPlaylist>,
) {
    companion object {
        const val VERSION = 0
    }
}

private const val PLAYLISTS = "playlists"