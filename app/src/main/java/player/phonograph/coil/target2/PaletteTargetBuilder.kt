/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.target2

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Deferred
import player.phonograph.R
import player.phonograph.util.PaletteUtil.getColor

open class PaletteTargetBuilder(protected open val defaultColor: Int) {

    constructor(context: Context) : this(context.getColor(R.color.defaultFooterColor))

    private var onSuccess: (result: Drawable, paletteColor: Int) -> Unit = { _, _ -> }
    fun onResourceReady(block: (result: Drawable, paletteColor: Int) -> Unit): PaletteTargetBuilder =
        this.apply {
            onSuccess = block
        }

    private var onFail: (error: Drawable?) -> Unit = {}
    fun onFail(block: (error: Drawable?) -> Unit): PaletteTargetBuilder =
        this.apply {
            onFail = block
        }

    private var onStart: (placeholder: Drawable?) -> Unit = {}
    fun onStart(block: (placeholder: Drawable?) -> Unit): PaletteTargetBuilder =
        this.apply {
            onStart = block
        }

    fun build(): BasePaletteTarget {
        return createBasePaletteTarget(
            onStart = onStart,
            onError = onFail,
            onSuccess = { result: Drawable, palette: Deferred<Palette>? ->
                palette?.getColor(defaultColor) {
                    onSuccess(result, it)
                }
            }
        )
    }
}
