/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics

@JvmInline
value class LyricsSource(val type: Int = UNKNOWN_SOURCE) {
    @Suppress("FunctionName")
    companion object {
        fun Embedded() = LyricsSource(EMBEDDED)
        fun ExternalPrecise() = LyricsSource(EXTERNAL_PRECISE)
        fun ExternalDecorated() = LyricsSource(EXTERNAL_DECORATED)

        const val EMBEDDED = 0
        const val EXTERNAL_PRECISE = 1
        const val EXTERNAL_DECORATED = 2

        fun Unknown() = LyricsSource(UNKNOWN_SOURCE)
        const val UNKNOWN_SOURCE = -1
    }
}