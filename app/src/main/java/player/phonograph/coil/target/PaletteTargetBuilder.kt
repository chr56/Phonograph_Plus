/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.target

import player.phonograph.coil.target.PaletteUtil.getColor
import player.phonograph.util.theme.themeFooterColor
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

open class PaletteTargetBuilder(protected open val defaultColor: Int) {

    private var onResourceReady: (result: Drawable, paletteColor: Int) -> Unit = { _, _ -> }
    fun onResourceReady(block: (result: Drawable, paletteColor: Int) -> Unit): PaletteTargetBuilder =
        this.apply {
            onResourceReady = block
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

    fun build(): PaletteDelegateTarget {
        return PaletteDelegateTarget.create(
            onStart = onStart,
            onError = onFail,
            onSuccess = { result: Drawable, palette: Deferred<Palette>? ->
                coroutineScope.launch {
                    val color = palette?.getColor(defaultColor) ?: defaultColor
                    withContext(Dispatchers.Main.immediate) {
                        onResourceReady(result, color)
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
