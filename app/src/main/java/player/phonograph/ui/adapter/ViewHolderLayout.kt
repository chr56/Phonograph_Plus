/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.R
import player.phonograph.ui.adapter.ViewHolderLayout.Companion.from
import androidx.annotation.IntDef
import androidx.annotation.LayoutRes

@IntDef(
    ViewHolderLayout.TYPE_CUSTOM,
    ViewHolderLayout.TYPE_LIST,
    ViewHolderLayout.TYPE_LIST_EXTENDED,
    ViewHolderLayout.TYPE_LIST_SINGLE_ROW,
    ViewHolderLayout.TYPE_LIST_NO_IMAGE,
    ViewHolderLayout.TYPE_LIST_3L,
    ViewHolderLayout.TYPE_LIST_3L_EXTENDED,
    ViewHolderLayout.TYPE_GRID,
    ViewHolderLayout.TYPE_GRID_CARD_HORIZONTAL,
)
@Retention(AnnotationRetention.BINARY)
annotation class ViewHolderType

/**
 * value class for ViewHolder's layout
 *
 * use [from] or static value to create
 */
@JvmInline
value class ViewHolderLayout private constructor(@ViewHolderType val ordinal: Int) {

    /**
     * Layout resource id
     */
    @LayoutRes
    fun layout(): Int = when (ordinal) {
        TYPE_LIST                 -> R.layout.item_list
        TYPE_LIST_EXTENDED        -> R.layout.item_list_extended
        TYPE_LIST_SINGLE_ROW      -> R.layout.item_list_single_row
        TYPE_LIST_NO_IMAGE        -> R.layout.item_list_no_image
        TYPE_LIST_3L              -> R.layout.item_list_3l
        TYPE_LIST_3L_EXTENDED     -> R.layout.item_list_3l_extended
        TYPE_GRID                 -> R.layout.item_grid
        TYPE_GRID_CARD_HORIZONTAL -> R.layout.item_grid_card_horizontal
        else                      -> R.layout.item_list //default
    }

    companion object {


        const val TYPE_LIST = 1
        const val TYPE_LIST_EXTENDED = 2
        const val TYPE_LIST_SINGLE_ROW = 5
        const val TYPE_LIST_NO_IMAGE = 6
        const val TYPE_LIST_3L = 8
        const val TYPE_LIST_3L_EXTENDED = 9

        const val TYPE_GRID = 12
        const val TYPE_GRID_CARD_HORIZONTAL = 15

        const val TYPE_CUSTOM = 0

        val LIST: ViewHolderLayout
            get() = ViewHolderLayout(TYPE_LIST)
        val LIST_EXTENDED: ViewHolderLayout
            get() = ViewHolderLayout(TYPE_LIST_EXTENDED)
        val LIST_SINGLE_ROW: ViewHolderLayout
            get() = ViewHolderLayout(TYPE_LIST_SINGLE_ROW)
        val LIST_NO_IMAGE: ViewHolderLayout
            get() = ViewHolderLayout(TYPE_LIST_NO_IMAGE)
        val LIST_3L: ViewHolderLayout
            get() = ViewHolderLayout(TYPE_LIST_3L)
        val LIST_3L_EXTENDED: ViewHolderLayout
            get() = ViewHolderLayout(TYPE_LIST_3L_EXTENDED)
        val GRID: ViewHolderLayout
            get() = ViewHolderLayout(TYPE_GRID)
        val GRID_CARD_HORIZONTAL: ViewHolderLayout
            get() = ViewHolderLayout(TYPE_GRID_CARD_HORIZONTAL)

        fun from(@ViewHolderType type: Int): ViewHolderLayout = when (type) {
            TYPE_LIST                 -> LIST
            TYPE_LIST_EXTENDED        -> LIST_EXTENDED
            TYPE_LIST_SINGLE_ROW      -> LIST_SINGLE_ROW
            TYPE_LIST_NO_IMAGE        -> LIST_NO_IMAGE
            TYPE_LIST_3L              -> LIST_3L
            TYPE_LIST_3L_EXTENDED     -> LIST_3L_EXTENDED
            TYPE_GRID                 -> GRID
            TYPE_GRID_CARD_HORIZONTAL -> GRID_CARD_HORIZONTAL
            else                      -> ViewHolderLayout(TYPE_CUSTOM)
        }

    }
}