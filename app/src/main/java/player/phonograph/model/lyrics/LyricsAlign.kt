/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.model.lyrics

import androidx.annotation.StringDef


const val LYRICS_ALIGN_LEFT = "left"
const val LYRICS_ALIGN_RIGHT = "right"
const val LYRICS_ALIGN_CENTER = "center"

@StringDef(
    LYRICS_ALIGN_CENTER,
    LYRICS_ALIGN_LEFT,
    LYRICS_ALIGN_RIGHT,
)
@Retention(AnnotationRetention.SOURCE)
annotation class LyricsAlign