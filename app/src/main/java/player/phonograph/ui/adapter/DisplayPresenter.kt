/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.sort.SortMode
import android.content.Context

interface DisplayPresenter<T> {

    fun getItemID(item: T): Long

    fun getDisplayTitle(context: Context, item: T): CharSequence
    fun getSecondaryText(context: Context, item: T): CharSequence
    fun getTertiaryText(context: Context, item: T): CharSequence? = null

    fun getDescription(context: Context, item: T): CharSequence? = null

    fun getSectionText(item: T, sortMode: SortMode?): String? = null
    fun getOrdinalText(item: T, position: Int): String? = null

    fun getDefaultArtwork(item: T): Int = -1
    fun hasArtwork(item: T): Boolean = false

    fun clickAction(context: Context, item: T, position: Int, list: List<T>): ActionEntry? = null
    fun menuEntries(context: Context, item: T, position: Int): List<ActionEntry> = emptyList()

}