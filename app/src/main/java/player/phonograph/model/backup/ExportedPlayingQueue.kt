/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.backup

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class ExportedPlayingQueue(
    @SerialName("version") val version: Int,
    @SerialName(PLAYING_QUEUE) val playingQueue: List<ExportedSong>,
    @SerialName(ORIGINAL_PLAYING_QUEUE) val originalPlayingQueue: List<ExportedSong>,
) {
    companion object {
        const val VERSION = 0
    }
}

private const val PLAYING_QUEUE = "playing_queue"
private const val ORIGINAL_PLAYING_QUEUE = "original_playing_queue"
