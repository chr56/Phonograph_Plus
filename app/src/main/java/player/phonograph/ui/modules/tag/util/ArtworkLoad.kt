/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.util

import player.phonograph.coil.loadImage
import player.phonograph.coil.retriever.PARAMETERS_RAW
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.util.theme.themeFooterColor
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun loadCover(context: Context, data: Any): Pair<Bitmap?, Color?> =
    suspendCancellableCoroutine { continuation ->
        loadImage(context) {
            data(data)
            parameters(PARAMETERS_RAW)
            target(
                PaletteTargetBuilder()
                    .defaultColor(themeFooterColor(context))
                    .onResourceReady { result: Drawable, paletteColor: Int ->
                        continuation.resume(result.toBitmap() to Color(paletteColor)) { _, _, _ -> }
                    }
                    .onFail {
                        continuation.resume(null to null) { _, _, _ -> }
                    }
                    .build()
            )
        }
    }