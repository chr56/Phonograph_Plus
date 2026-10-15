/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.model.sort.SortMode
import player.phonograph.model.ui.ItemLayoutStyle

data class DisplayOptions(
    val layoutStyle: ItemLayoutStyle,
    val sortMode: SortMode?,
    val usePalette: Boolean,
    val showMenu: Boolean = true,
    val showSectionName: Boolean = true,
)