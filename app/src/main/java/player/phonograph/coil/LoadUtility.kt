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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
