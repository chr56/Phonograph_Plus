/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.coil.cache

import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import player.phonograph.coil.PARAMETERS_KEY_CACHE
import player.phonograph.coil.palette.PaletteBitmapDrawable
import player.phonograph.coil.quickCache
import player.phonograph.coil.retriever.collectCacheSetting
import player.phonograph.model.Song
import androidx.collection.LruCache

class CacheInterceptor : Interceptor {

    private val lruCache = LruCache<Long, PaletteBitmapDrawable>(8)

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {

        val enableCache = collectCacheSetting(chain.request.context)

        val request = chain.request
        if (request.parameters.quickCache(false)) {
            val data = request.data
            if (data is Song) {
                return withQuickCache(data, enableCache, chain)
            }
        }

        val newRequest = withParameterCache(chain.request, enableCache)
        return chain.proceed(newRequest)
    }

    /**
     * An in-memory LRU cache is used to process the chain for _Song_
     */
    private suspend fun withQuickCache(data: Song, enableNormalCache: Boolean, chain: Interceptor.Chain): ImageResult {
        val key = data.id
        // check cache
        val cached = lruCache[key]
        if (cached != null) {
            return SuccessResult(cached, chain.request, DataSource.MEMORY)
        }
        // update cache
        val imageResult = chain.proceed(withParameterCache(chain.request, enableNormalCache))
        val drawable = imageResult.drawable
        if (imageResult is SuccessResult && drawable is PaletteBitmapDrawable) {
            lruCache.put(key, drawable)
        }
        return imageResult
    }

    private fun withParameterCache(request: ImageRequest, enableCache: Boolean): ImageRequest =
        request.newBuilder()
            .setParameter(PARAMETERS_KEY_CACHE, enableCache)
            .build()
}