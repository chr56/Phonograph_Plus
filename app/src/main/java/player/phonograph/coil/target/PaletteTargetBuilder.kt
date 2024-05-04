/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.target

import player.phonograph.R
import player.phonograph.coil.target.PaletteUtil.getColor
import androidx.palette.graphics.Palette
import android.content.Context
import android.graphics.drawable.Drawable
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

open class PaletteTargetBuilder(protected open val defaultColor: Int) {

    constructor(context: Context) : this(context.getColor(R.color.footer_background))

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

    private var yieldCondition: () -> Boolean = { true }
    fun withConditionalYield(condition: () -> Boolean): PaletteTargetBuilder =
        this.apply {
            yieldCondition = condition
        }

    fun build(): BasePaletteTarget {
        return createBasePaletteTarget(
            onStart = onStart,
            onError = onFail,
            onSuccess = { result: Drawable, palette: Deferred<Palette>? ->
                coroutineScope.launch {
                    val color = palette?.getColor(defaultColor) ?: defaultColor
                    while (!yieldCondition()) yield()
                    withContext(Dispatchers.Main) {
                        onSuccess(result, color)
                    }
                }
            }
        )
    }

    companion object {
        private val coroutineScope =
            CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName("PaletteGenerator"))
    }
}
