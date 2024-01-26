/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.coil

import androidx.annotation.IntDef
import androidx.collection.LruCache
import android.content.Context
import android.util.LongSparseArray

abstract class AbsPreloadImageCache<K, G : Any>(size: Int, @CacheImplementation type: Int) {

    private val cache: Cache<G> = when (type) {
        IMPL_LRU          -> Cache.LruCacheImpl(size)
        IMPL_SPARSE_ARRAY -> Cache.SparseArrayCacheImpl(size)
        else              -> throw IllegalArgumentException("Unknown cache type: $type")
    }

    private suspend fun loadAndStore(context: Context, key: K): G {
        val loaded: G = load(context, key)
        cache[id(key)] = loaded
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

    private sealed interface Cache<V> {
        operator fun get(key: Long): V?
        operator fun set(key: Long, value: V)

        class LruCacheImpl<V : Any>(cacheSize: Int) : Cache<V> {
            private val cache: LruCache<Long, V> = LruCache(cacheSize)
            override fun get(key: Long): V? = cache[key]
            override fun set(key: Long, value: V) {
                cache.put(key, value)
            }

        }

        class SparseArrayCacheImpl<V>(initialCapacity: Int) : Cache<V> {
            private val cache: LongSparseArray<V> = LongSparseArray<V>(initialCapacity)
            override fun get(key: Long): V = cache[key]
            override fun set(key: Long, value: V) = cache.put(key, value)
        }
    }

    companion object {
        const val IMPL_LRU = 0
        const val IMPL_SPARSE_ARRAY = 1

        @IntDef(IMPL_LRU, IMPL_SPARSE_ARRAY)
        @Retention(AnnotationRetention.SOURCE)
        @Target(AnnotationTarget.VALUE_PARAMETER)
        annotation class CacheImplementation
    }
}