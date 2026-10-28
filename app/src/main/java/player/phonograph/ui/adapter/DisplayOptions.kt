/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.model.sort.SortMode
import player.phonograph.model.ui.ItemLayoutStyle
import androidx.annotation.IntDef

data class DisplayOptions(
    val layoutStyle: ItemLayoutStyle,
    val sortMode: SortMode?,
    val usePalette: Boolean,
    val showMenu: Boolean = true,
    val showSectionName: Boolean = true,
    @ImageryType val imageryType: Int = IMAGE,
) {
    companion object {
        const val NONE = 0
        const val ORDINAL_TEXT = 1
        const val ICON = 2
        const val IMAGE = 3

        @IntDef(NONE, ORDINAL_TEXT, ICON, IMAGE)
        @Retention(AnnotationRetention.SOURCE)
        annotation class ImageryType
    }
}