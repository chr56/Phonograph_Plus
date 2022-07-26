/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.target

import android.graphics.drawable.Drawable
import androidx.palette.graphics.Palette
import player.phonograph.App
import player.phonograph.R
import player.phonograph.util.PhonographColorUtil
import util.mddesign.util.Util

abstract class PhonographColoredTarget : ColoredTarget() {

    override fun onReady(drawable: Drawable, palette: Palette?) {
        onReady(drawable, PhonographColorUtil.getColor(palette, defaultFooterColor))
    }

    /**
     * more specific
     */
    abstract fun onReady(drawable: Drawable, color: Int)

    protected open val defaultFooterColor: Int =
        App.instance.getColor(R.color.defaultFooterColor)
}
