/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.ViewHolderType

data class ConstDisplayConfig(
    @ViewHolderType override var layoutStyle: ItemLayoutStyle,
    override val usePalette: Boolean = false,
    override val showSectionName: Boolean = true,
    override val imageType: Int = DisplayConfig.IMAGE_TYPE_IMAGE
) : DisplayConfig