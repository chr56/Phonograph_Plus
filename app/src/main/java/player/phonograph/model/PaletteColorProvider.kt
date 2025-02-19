/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model

import kotlinx.coroutines.flow.StateFlow

interface IPaletteColorProvider {
    val paletteColor: StateFlow<Int>
}