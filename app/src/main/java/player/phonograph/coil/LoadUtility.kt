/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil

import android.content.Context
import android.widget.ImageView
import coil.Coil
import coil.request.ImageRequest
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
