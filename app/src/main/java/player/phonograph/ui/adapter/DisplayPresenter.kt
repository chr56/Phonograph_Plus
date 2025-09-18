/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.adapter

import coil.request.Disposable
import coil.target.Target
import player.phonograph.model.sort.SortMode
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.ui.actions.ActionMenuProviders
import player.phonograph.ui.actions.ClickActionProviders
import androidx.annotation.IntDef
import android.content.Context
import android.graphics.drawable.Drawable

interface DisplayPresenter<T> {

    fun getItemID(item: T): Long

    fun getDisplayTitle(context: Context, item: T): CharSequence
    fun getSecondaryText(context: Context, item: T): CharSequence
    fun getTertiaryText(context: Context, item: T): CharSequence? = null

    fun getDescription(context: Context, item: T): CharSequence? = null

    val clickActionProvider: ClickActionProviders.ClickActionProvider<T>
    val menuProvider: ActionMenuProviders.ActionMenuProvider<T>? get() = null

    fun getRelativeOrdinalText(item: T): String? = null

    val showSectionName: Boolean get() = true

    fun getSortOrderKey(context: Context): SortMode? = null

    fun getSortOrderReference(item: T, sortMode: SortMode): String? = null

    fun getNonSortOrderReference(item: T): String? = null

    val layoutStyle: ItemLayoutStyle

    @ImageType
    val imageType: Int

    val usePalette: Boolean

    fun getIcon(context: Context, item: T): Drawable? = null

    fun startLoadingImage(context: Context, item: T, target: Target): Disposable? = null

    companion object {
        const val IMAGE_TYPE_NONE = 0
        const val IMAGE_TYPE_FIXED_ICON = 1
        const val IMAGE_TYPE_IMAGE = 2
        const val IMAGE_TYPE_TEXT = 4

        @IntDef(IMAGE_TYPE_NONE, IMAGE_TYPE_FIXED_ICON, IMAGE_TYPE_IMAGE, IMAGE_TYPE_TEXT)
        @Retention(AnnotationRetention.SOURCE)
        annotation class ImageType
    }
}