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
import coil.size.SizeResolver
import coil.target.Target
import androidx.annotation.DrawableRes
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


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

    fun into(
        onStart: (placeholder: Drawable?) -> Unit = {},
        onError: (error: Drawable?) -> Unit = {},
        onSuccess: (result: Drawable) -> Unit = {},
    ): ChainBuilder {
        requestBuilder.target(onStart, onError, onSuccess)
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

    fun size(resolver: SizeResolver): ChainBuilder {
        requestBuilder.size(resolver)
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
