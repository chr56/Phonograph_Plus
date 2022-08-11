/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model

import android.content.Context

interface Displayable {
    fun getItemID(): Long

    fun getDisplayTitle(context: Context): CharSequence
    fun getDescription(context: Context): CharSequence?
}
