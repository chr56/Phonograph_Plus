/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.model.metadata

import player.phonograph.model.Song

/**
 * Extract metadata (music tags) for audio files
 */
interface MetadataExtractor {

    /**
     * read metadata via [path] of an audio file
     */
    fun extractMetadata(path: String, collector: ExceptionCollector?): AudioMetadata?

    /**
     * read metadata via [song] instance
     */
    fun extractMetadata(song: Song, collector: ExceptionCollector?): AudioMetadata?


    /**
     * read embed lyrics via [path] of an audio file
     */
    fun extractLyrics(path: String, collector: ExceptionCollector?): String?

    /**
     * read embed image via [path] of an audio file
     */
    fun extractRawImage(path: String, collector: ExceptionCollector?): ByteArray?


}