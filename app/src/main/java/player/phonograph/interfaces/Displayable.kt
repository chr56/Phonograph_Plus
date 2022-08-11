/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.interfaces

interface Displayable {
    fun getItemID(): Long

    fun getDisplayTitle(): CharSequence
    fun getDescription(): CharSequence?
}
