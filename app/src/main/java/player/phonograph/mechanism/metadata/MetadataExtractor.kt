/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.metadata

import player.phonograph.model.Song
import player.phonograph.model.metadata.AudioMetadata
import android.content.Context

/**
 * Extract metadata (tags) for songs
 */
sealed interface MetadataExtractor {
    /**
     * read metadata of [song]
     */
    fun extractSongMetadata(context: Context, song: Song): AudioMetadata?
}


