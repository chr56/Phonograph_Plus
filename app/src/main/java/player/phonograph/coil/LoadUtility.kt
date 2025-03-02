/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil

import coil.Coil
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.Parameters
import coil.size.Dimension
import coil.size.Size
import coil.target.Target
import player.phonograph.R
import player.phonograph.coil.palette.PaletteColorTarget
import player.phonograph.model.PaletteBitmap
import player.phonograph.model.Song
import player.phonograph.util.theme.themeFooterColor
import player.phonograph.util.withTimeoutOrNot
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun loadImage(context: Context, song: Song, timeout: Long): PaletteBitmap =
    try {
        withTimeoutOrNot(timeout, Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                loadImage(context, song) { _, drawable, color ->
                    if (drawable is BitmapDrawable) {
                        continuation.resume(PaletteBitmap(drawable.bitmap, color)) { tr, _, _ -> cancel("", tr) }
                    } else {
                        continuation.cancel()
                    }
                }
            }
        }
    } catch (e: TimeoutCancellationException) {
        PaletteBitmap(
            AppCompatResources.getDrawable(context, R.drawable.default_album_art)!!.toBitmap(),
            themeFooterColor(context)
        )
    }


fun loadImage(
    context: Context,
    song: Song,
    colorCallback: (Song, Drawable, Int) -> Unit,
) {
    loadImage(context)
        .from(song)
        .withPalette()
        .into(
            PaletteColorTarget(
                success = { result, palette -> colorCallback(song, result, palette) },
                defaultColor = themeFooterColor(context),
            )
        )
        .enqueue()
}

inline fun loadImage(context: Context, cfg: ImageRequest.Builder.() -> Unit) {
    Coil.imageLoader(context).enqueue(
        ImageRequest.Builder(context).apply(cfg).build()
    )
}

fun loadImage(context: Context): ChainBuilder = ChainBuilder(context)

class ChainBuilder internal constructor(context: Context) {
    private val loader = Coil.imageLoader(context)
    private val requestBuilder: ImageRequest.Builder = ImageRequest.Builder(context)

    fun from(data: Any?): ChainBuilder {
        requestBuilder.data(data)
        return this
    }

    fun into(view: ImageView): ChainBuilder {
        requestBuilder.target(view)
        return this
    }

    fun into(target: Target): ChainBuilder {
        requestBuilder.target(target)
        return this
    }

    fun config(block: ImageRequest.Builder.() -> Unit): ChainBuilder {
        requestBuilder.apply(block)
        return this
    }

    fun default(@DrawableRes res: Int): ChainBuilder {
        requestBuilder.placeholder(res)
        requestBuilder.error(res)
        return this
    }

    fun default(drawable: Drawable?): ChainBuilder {
        requestBuilder.placeholder(drawable)
        requestBuilder.error(drawable)
        return this
    }

    fun size(size: Size): ChainBuilder {
        requestBuilder.size(size)
        return this
    }

    fun size(width: Dimension, height: Dimension): ChainBuilder {
        requestBuilder.size(Size(width, height))
        return this
    }

    fun parameters(parameters: Parameters): ChainBuilder {
        requestBuilder.parameters(parameters)
        return this
    }

    fun withPalette(): ChainBuilder {
        requestBuilder.parameters(Parameters.Builder().set(PARAMETERS_KEY_PALETTE, true).build())
        return this
    }

    private val request get() = requestBuilder.build()
    fun enqueue(): Disposable = loader.enqueue(request)

    suspend fun execute(): ImageResult = loader.execute(request)

    fun execute(coroutineScope: CoroutineScope): Job =
        coroutineScope.launch {
            loader.execute(request)
        }
}
