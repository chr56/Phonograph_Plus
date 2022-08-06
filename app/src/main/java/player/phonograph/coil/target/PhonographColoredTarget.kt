/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.target

import android.graphics.drawable.Drawable
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Deferred
import player.phonograph.App
import player.phonograph.R
import player.phonograph.coil.PaletteUtil.getColor

abstract class PhonographColoredTarget : ColoredTarget() {

    override fun onReady(drawable: Drawable, palette: Deferred<Palette>?) {
        onResourcesReady(drawable)
        palette?.getColor(defaultFooterColor, ::onColorReady)
    }

    abstract fun onResourcesReady(drawable: Drawable)
    abstract fun onColorReady(color: Int)

    protected open val defaultFooterColor: Int =
        App.instance.getColor(R.color.defaultFooterColor)
}
