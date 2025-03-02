/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.util

import coil.request.Parameters
import player.phonograph.coil.loadImage
import player.phonograph.coil.palette.PaletteColorTarget
import player.phonograph.coil.palette.PaletteInterceptor.Companion.PARAMETERS_KEY_PALETTE
import player.phonograph.coil.retriever.PARA_KEY_RAW
import player.phonograph.util.theme.themeFooterColor
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun loadCover(context: Context, data: Any): Pair<Bitmap?, Color?> =
    suspendCancellableCoroutine { continuation ->
        loadImage(context)
            .from(data)
            .parameters(Parameters.Builder().set(PARA_KEY_RAW, true).set(PARAMETERS_KEY_PALETTE, true).build())
            .into(
                PaletteColorTarget(
                    defaultColor = themeFooterColor(context),
                    success = { result: Drawable, paletteColor: Int ->
                        continuation.resume(result.toBitmap() to Color(paletteColor)) { _, _, _ -> }
                    },
                    error = { _, _ ->
                        continuation.resume(null to null) { _, _, _ -> }
                    },
                )
            )
    }