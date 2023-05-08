/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.Coil
import coil.request.ImageRequest
import coil.size.Dimension
import coil.size.Size
import coil.target.Target
import player.phonograph.R
import player.phonograph.coil.target.PaletteBitmap
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Song
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import android.graphics.drawable.BitmapDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun loadImage(context: Context, song: Song): PaletteBitmap = try {
    withTimeout(2000) {
        suspendCancellableCoroutine { continuation ->
            loadImage(context, song) { _, drawable, color ->
                require(drawable is BitmapDrawable)
                continuation.resume(PaletteBitmap(drawable.bitmap, color)) { cancel() }
            }
        }
    }
} catch (e: TimeoutCancellationException) {
    PaletteBitmap(
        AppCompatResources.getDrawable(context, R.drawable.default_album_art)!!.toBitmap(),
        context.getColor(R.color.defaultFooterColor)
    )
}


fun loadImage(
    context: Context,
    song: Song,
    colorCallback: (Song, Drawable, Int) -> Unit,
) {
    loadImage(context)
        .from(song)
        .into(
            PaletteTargetBuilder(context)
                .onResourceReady { result, palette ->
                    colorCallback(song, result, palette)
                }
                .build()
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
        return this
    }
    fun default(drawable: Drawable): ChainBuilder {
        requestBuilder.placeholder(drawable)
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

    private val request get() = requestBuilder.build()
    fun enqueue() {
        loader.enqueue(request)
    }
    suspend fun execute() {
        loader.execute(request)
    }
    fun execute(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            loader.execute(request)
        }
    }
}
