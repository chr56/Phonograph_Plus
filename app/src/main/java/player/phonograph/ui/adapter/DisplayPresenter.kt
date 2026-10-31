/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.adapter

import coil.request.Disposable
import coil.target.Target
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.sort.SortMode
import android.content.Context
import android.graphics.drawable.Drawable

interface DisplayPresenter<T> {

    fun getItemID(item: T): Long

    fun getDisplayTitle(context: Context, item: T): CharSequence
    fun getSecondaryText(context: Context, item: T): CharSequence
    fun getTertiaryText(context: Context, item: T): CharSequence? = null

    fun getDescription(context: Context, item: T): CharSequence? = null

    fun getSectionText(item: T, sortMode: SortMode?): String? = null
    fun getOrdinalText(item: T, position: Int): String? = null

    fun clickAction(context: Context, item: T, position: Int, list: List<T>): ActionEntry? = null
    fun menuEntries(context: Context, item: T, position: Int): List<ActionEntry> = emptyList()

    fun getIcon(context: Context, item: T): Drawable? = null
    fun loadImageInto(context: Context, item: T, target: Target): Disposable? = null
}