/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.target

import android.graphics.drawable.Drawable
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Deferred

abstract class ColoredTarget : PaletteTarget() {

    override fun onSuccess(result: Drawable) {
        super.onSuccess(result)
        onReady(drawable = result, palette = palette)
    }

    /**
     * call only when success
     * @param drawable fetched result by coil
     * @param palette null if drawable can not generate to palette
     */
    abstract fun onReady(drawable: Drawable, palette: Deferred<Palette>?)
}
