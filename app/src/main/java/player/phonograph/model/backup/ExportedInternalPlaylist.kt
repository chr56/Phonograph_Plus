/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.backup

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
class ExportedInternalPlaylist(
    @SerialName("name") val name: String,
    @SerialName("songs") val songs: List<ExportedSong>,
    @SerialName("date_added") val dateAdded: Long = 0,
    @SerialName("date_modified") val dateModified: Long = 0,
)

