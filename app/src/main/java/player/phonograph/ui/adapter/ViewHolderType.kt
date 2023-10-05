/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.R
import androidx.annotation.IntDef
import androidx.annotation.LayoutRes

@IntDef(
    ViewHolderTypes.CUSTOM,
    ViewHolderTypes.LIST,
    ViewHolderTypes.LIST_SINGLE_ROW,
    ViewHolderTypes.LIST_NO_IMAGE,
    ViewHolderTypes.GRID,
    ViewHolderTypes.GRID_CARD_HORIZONTAL,
)
@Retention(AnnotationRetention.SOURCE)
annotation class ViewHolderType

object ViewHolderTypes {
    const val LIST = 1
    const val LIST_SINGLE_ROW = 2
    const val LIST_NO_IMAGE = 3
    const val GRID = 5
    const val GRID_CARD_HORIZONTAL = 6

    const val CUSTOM = 0

    @LayoutRes
    fun layout(@ViewHolderType type: Int): Int = when (type) {
        LIST                 -> R.layout.item_list
        LIST_SINGLE_ROW      -> R.layout.item_list_single_row
        LIST_NO_IMAGE        -> R.layout.item_list_no_image
        GRID                 -> R.layout.item_grid
        GRID_CARD_HORIZONTAL -> R.layout.item_grid_card_horizontal
        else                 -> 0
    }
}