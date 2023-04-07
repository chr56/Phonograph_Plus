/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.target

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*
import player.phonograph.R
import player.phonograph.coil.target.PaletteUtil.getColor

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
                coroutineScope.launch {
                    val color = palette?.getColor(defaultColor) ?: defaultColor
                    withContext(Dispatchers.Main) {
                        onSuccess(result, color)
                    }
                }
            }
        )
    }

    companion object {
        private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }
}
