/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.service

data class MusicServiceStatus(
    val isPlaying: Boolean,
    val shuffleMode: ShuffleMode,
    val repeatMode: RepeatMode,
)