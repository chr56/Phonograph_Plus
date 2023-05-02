/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics

import player.phonograph.R
import android.content.Context

@JvmInline
value class LyricsSource(val type: Int = UNKNOWN_SOURCE) {
    @Suppress("FunctionName")
    companion object {
        fun Embedded() = LyricsSource(EMBEDDED)
        fun ExternalPrecise() = LyricsSource(EXTERNAL_PRECISE)
        fun ExternalDecorated() = LyricsSource(EXTERNAL_DECORATED)
        fun ManuallyLoaded() = LyricsSource(MANUALLY_LOADED)

        const val EMBEDDED = 0
        const val EXTERNAL_PRECISE = 1
        const val EXTERNAL_DECORATED = 2
        const val MANUALLY_LOADED = 4

        fun Unknown() = LyricsSource(UNKNOWN_SOURCE)
        const val UNKNOWN_SOURCE = -1
    }

    fun name(context: Context): String = when (type) {
        EMBEDDED                             -> context.getString(R.string.embedded_lyrics)
        EXTERNAL_DECORATED, EXTERNAL_PRECISE -> context.getString(R.string.external_lyrics)
        MANUALLY_LOADED                      -> context.getString(R.string.loaded)
        else                                 -> "unknown"
    }
}