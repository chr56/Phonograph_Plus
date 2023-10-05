/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

interface DisplayConfig {

    @ViewHolderType val layoutType: Int

    val usePalette: Boolean

    val showSectionName: Boolean

    val useImageText: Boolean
}