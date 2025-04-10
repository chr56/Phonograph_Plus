/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.ui

import kotlinx.coroutines.flow.StateFlow

interface PaletteColorProvider {
    val paletteColor: StateFlow<Int>
}