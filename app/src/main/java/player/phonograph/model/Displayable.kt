/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model

interface Displayable {
    fun getItemID(): Long

    fun getDisplayTitle(): CharSequence
    fun getDescription(): CharSequence?
}
