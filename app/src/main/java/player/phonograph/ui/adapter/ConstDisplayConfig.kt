/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

data class ConstDisplayConfig(
    @ViewHolderType override var layoutType: ViewHolderLayout,
    override val usePalette: Boolean = false,
    override val showSectionName: Boolean = true,
    override val useImageText: Boolean = false,
) : DisplayConfig