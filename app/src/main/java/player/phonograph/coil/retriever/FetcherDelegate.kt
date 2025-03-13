/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.coil.retriever

import coil.fetch.FetchResult
import coil.size.Size
import player.phonograph.coil.cache.CacheStore
import player.phonograph.coil.model.LoaderTarget
import player.phonograph.mechanism.setting.CoilImageConfig
import player.phonograph.util.debug
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class FetcherDelegate<T : LoaderTarget, R : ImageRetriever> {

    @Suppress("unused")
    protected fun enableCache(context: Context): Boolean = CoilImageConfig.enableImageCache

    abstract val retriever: R

    abstract val cacheStore: CacheStore.Cache<T>

    suspend fun retrieve(
        target: T,
        context: Context,
        size: Size,
        rawImage: Boolean,
    ): FetchResult? {

        if (enableCache(context)) {
            val noSpecificImage = cacheStore.isNoImage(target, retriever.id)
            if (noSpecificImage) return null

            val cached = cacheStore.get(target, retriever.id)
            if (cached != null) {
                debug {
                    Log.v(TAG, "Image was read from cache of ${retriever.name} for file $target")
                }
                return cached
            }
        }


        val result = retrieveImpl(target, context, size, rawImage)
        return if (result != null) {

            if (enableCache(context)) {
                coroutineScope.launch {
                    cacheStore.set(target, result, retriever.id)
                }
            }

            result
        } else {
            debug {
                Log.v(TAG, "Image not available from ${retriever.name} for $target")
            }

            if (enableCache(context)) {
                cacheStore.markNoImage(target, retriever.id)
            }

            null
        }
    }

    protected abstract suspend fun retrieveImpl(
        target: T,
        context: Context,
        size: Size,
        rawImage: Boolean,
    ): FetchResult?

    protected val coroutineScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    companion object {
        private const val TAG = "FetcherDelegate"
    }
}