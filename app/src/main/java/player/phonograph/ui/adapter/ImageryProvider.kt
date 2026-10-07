/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.adapter

import coil.request.Disposable
import coil.target.Target
import player.phonograph.R
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import android.content.Context
import android.graphics.drawable.Drawable

sealed interface ImageryProvider<in T> {

    object None : ImageryProvider<Any?>

    object Text : ImageryProvider<Any?>

    enum class FixedIcon(@DrawableRes val res: Int) : ImageryProvider<Any?> {
        DEFAULT(R.drawable.default_album_art),
        ARTIST(R.drawable.default_artist_image),
        FOLDER(R.drawable.ic_folder_white_24dp),
        FILE(R.drawable.ic_file_music_white_24dp),
        ;

        fun getIcon(context: Context): Drawable = AppCompatResources.getDrawable(context, res)!!
    }

    sealed interface AdaptiveIcon<in T> : ImageryProvider<T> {
        fun getIcon(context: Context, item: T): Drawable
    }

    sealed interface Artwork<in T> : ImageryProvider<T> {
        val usePalette: Boolean get() = false

        fun startLoadingImage(context: Context, item: T, target: Target): Disposable? = null
    }
}