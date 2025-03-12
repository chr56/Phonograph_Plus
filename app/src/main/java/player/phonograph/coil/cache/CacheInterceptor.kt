/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.coil.cache

import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.request.SuccessResult
import player.phonograph.coil.palette.PaletteBitmapDrawable
import player.phonograph.coil.quickCache
import player.phonograph.model.Song
import androidx.collection.LruCache

class CacheInterceptor : Interceptor {

    private val lruCache = LruCache<Long, PaletteBitmapDrawable>(8)

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {

        val request = chain.request
        if (request.parameters.quickCache(false)) {
            val data = request.data
            if (data is Song) {
                return withQuickCache(data, chain)
            }
        }

        return chain.proceed(request)
    }

    /**
     * An in-memory LRU cache is used to process the chain for _Song_
     */
    private suspend fun withQuickCache(data: Song, chain: Interceptor.Chain): ImageResult {
        val key = data.id
        // check cache
        val cached = lruCache[key]
        if (cached != null) {
            return SuccessResult(cached, chain.request, DataSource.MEMORY)
        }
        // update cache
        val imageResult = chain.proceed(chain.request)
        val drawable = imageResult.drawable
        if (imageResult is SuccessResult && drawable is PaletteBitmapDrawable) {
            lruCache.put(key, drawable)
        }
        return imageResult
    }
}