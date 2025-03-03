/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.coil.cache

import androidx.annotation.IntDef
import androidx.collection.LruCache
import androidx.collection.MutableScatterMap
import android.content.Context
import android.util.LongSparseArray

abstract class AbsPreloadImageCache<K, G : Any>(size: Int, @CacheImplementation type: Int) {


    protected abstract fun id(key: K): Long

    protected abstract suspend fun load(context: Context, key: K): G

    private suspend fun loadAndStore(context: Context, key: K): G {
        val loaded: G = load(context, key)
        cache[id(key)] = loaded
        return loaded
    }

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

    fun peek(key: K): G? = cache[id(key)]

    private val cache: Cache<G> = when (type) {
        IMPL_LRU          -> Cache.LruCacheImpl(size)
        IMPL_SPARSE_ARRAY -> Cache.SparseArrayCacheImpl(size)
        IMPL_SCATTER_MAP  -> Cache.ScatterMapCacheImpl(size)
        else              -> throw IllegalArgumentException("Unknown cache type: $type")
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
            override fun get(key: Long): V? = cache[key]
            override fun set(key: Long, value: V) = cache.put(key, value)
        }

        class ScatterMapCacheImpl<V>(initialCapacity: Int) : Cache<V> {
            private val cache: MutableScatterMap<Long, V> = MutableScatterMap(initialCapacity)
            override fun get(key: Long): V? = cache[key]
            override fun set(key: Long, value: V) {
                cache.put(key, value)
            }
        }
    }

    companion object {
        const val IMPL_LRU = 0
        const val IMPL_SPARSE_ARRAY = 1
        const val IMPL_SCATTER_MAP = 2

        @IntDef(IMPL_LRU, IMPL_SPARSE_ARRAY, IMPL_SCATTER_MAP)
        @Retention(AnnotationRetention.SOURCE)
        @Target(AnnotationTarget.VALUE_PARAMETER)
        annotation class CacheImplementation
    }
}