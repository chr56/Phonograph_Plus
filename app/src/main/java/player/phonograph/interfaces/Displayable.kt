/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.interfaces

import android.net.Uri
import androidx.annotation.MenuRes

interface Displayable {
    fun getItemID(): Long

    fun getTitle(): CharSequence
    fun getDescription(): CharSequence?
    fun getPic(): Uri?

    fun getSortOrderReference(): String?

    @MenuRes
    fun menuRes(): Int

    fun menuHandler():
        ((selection: Displayable, menuItemId: Int) -> Boolean)?

    fun multiMenuHandler():
        ((selection: List<Displayable>, menuItemId: Int) -> Boolean)?

    fun clickHandler(): () -> Unit
}
