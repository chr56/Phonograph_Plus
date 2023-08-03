/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.misc

import androidx.annotation.ColorInt
import kotlinx.coroutines.flow.StateFlow

interface IPaletteColorProvider {
    @get:ColorInt
    val paletteColor: StateFlow<Int>
}