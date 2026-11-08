/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.adapter

import androidx.annotation.IntDef

const val IMAGERY_NONE = 0
const val IMAGERY_ICON = 1
const val IMAGERY_IMAGE = 2
const val IMAGERY_ORDINAL_TEXT = 5
const val IMAGERY_CUSTOM_TEXT = 6

@IntDef(IMAGERY_NONE, IMAGERY_ICON, IMAGERY_IMAGE, IMAGERY_ORDINAL_TEXT, IMAGERY_CUSTOM_TEXT)
@Retention(AnnotationRetention.SOURCE)
annotation class ImageryType
