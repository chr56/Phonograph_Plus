/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.sort.SortMode
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.ui.actions.ClickActionProviders
import android.content.Context

interface DisplayPresenter<T> {

    fun getItemID(item: T): Long

    fun getDisplayTitle(context: Context, item: T): CharSequence
    fun getSecondaryText(context: Context, item: T): CharSequence
    fun getTertiaryText(context: Context, item: T): CharSequence? = null

    fun getDescription(context: Context, item: T): CharSequence? = null

    val clickActionProvider: ClickActionProviders.ClickActionProvider<T>

    val showMenu: Boolean get() = false
    fun menuEntries(context: Context, item: T, position: Int): List<ActionEntry> = emptyList()

    fun getRelativeOrdinalText(item: T): String? = null

    val showSectionName: Boolean get() = true

    fun getSortOrderKey(context: Context): SortMode? = null

    fun getSortOrderReference(item: T, sortMode: SortMode): String? = null

    fun getNonSortOrderReference(item: T): String? = null

    val layoutStyle: ItemLayoutStyle

    val imageryProvider: ImageryProvider<T>
}