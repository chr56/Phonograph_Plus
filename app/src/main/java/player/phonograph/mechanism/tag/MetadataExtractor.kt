/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.tag

import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import android.content.Context

/**
 * Extract metadata (tags) for songs
 */
sealed interface MetadataExtractor {
    /**
     * read metadata of [song]
     */
    fun extractSongMetadata(context: Context, song: Song): SongInfoModel?
}


