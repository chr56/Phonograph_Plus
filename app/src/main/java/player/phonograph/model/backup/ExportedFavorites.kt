/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.backup

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
class ExportedFavorites(
    @SerialName("version") val version: Int,
    @SerialName(FAVORITE_SONG) val favoriteSong: List<ExportedSong>,
    @SerialName(PINED_PLAYLIST) val pinedPlaylist: List<ExportedPlaylist>,
) {
    companion object {
        const val VERSION = 0
    }
}

private const val FAVORITE_SONG = "favorite"
private const val PINED_PLAYLIST = "pined_playlists"