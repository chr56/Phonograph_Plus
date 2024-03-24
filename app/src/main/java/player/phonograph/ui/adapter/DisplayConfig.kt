/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.model.ItemLayoutStyle
import androidx.annotation.IntDef

interface DisplayConfig {

    val layoutStyle: ItemLayoutStyle

    val usePalette: Boolean

    val showSectionName: Boolean

    val imageType: Int

    companion object {
        const val IMAGE_TYPE_FIXED_ICON = 1
        const val IMAGE_TYPE_IMAGE = 2
        const val IMAGE_TYPE_TEXT = 4

        @IntDef(IMAGE_TYPE_FIXED_ICON, IMAGE_TYPE_IMAGE, IMAGE_TYPE_TEXT)
        @Retention(AnnotationRetention.SOURCE)
        annotation class ImageType
    }
}