/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.coil

import androidx.collection.LruCache
import android.content.Context

abstract class AbsPreloadImageCache<K, G : Any>(size: Int) {

    private val cache: LruCache<Long, G> = LruCache(size)

    private suspend fun loadAndStore(context: Context, key: K): G {
        val loaded: G = load(context, key)
        cache.put(id(key), loaded)
        return loaded
    }

    protected abstract suspend fun load(context: Context, key: K): G

    protected abstract fun id(key: K): Long

    suspend fun fetch(context: Context, key: K): G {
        val cached: G? = cache[id(key)]
        return if (cached == null) {
            val loaded: G = loadAndStore(context, key)
            loaded
        } else {
            cached
        }
    }

    suspend fun preload(context: Context, key: K) {
        loadAndStore(context, key)
    }
}