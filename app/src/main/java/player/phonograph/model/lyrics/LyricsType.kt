/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.lyrics

import androidx.annotation.IntDef

@IntDef(LyricsType.TXT, LyricsType.LRC)
annotation class LyricsType {
    companion object {
        const val LRC: Int = 2
        const val TXT: Int = 1
    }
}