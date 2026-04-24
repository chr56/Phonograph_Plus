/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.repo.room.domain

import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.entity.ImageCacheEntity
import player.phonograph.repo.room.entity.ImageCacheEntity.CacheDomain

object RoomImageCache {

    private val database: MusicDatabase get() = MusicDatabase.koinInstance

    sealed interface FetchedCache {
        data object Empty : FetchedCache
        data object Unknown : FetchedCache
        data class Existed(val path: String) : FetchedCache

        fun isEmpty(): Boolean = this is Empty

        fun existedOrNull(): String? = if (this is Existed) path else null
    }

    fun fetch(@CacheDomain domain: Int, id: Long, source: Int): FetchedCache {
        val dao = database.ImageCacheDao()
        val cache = dao.obtain(domain, id, source) ?: return FetchedCache.Unknown

        val valid = (System.currentTimeMillis() - cache.timestamp) < TIME_OUT
        if (!valid) {
            dao.delete(domain, id, source)
        }

        return if (cache.empty) {
            FetchedCache.Empty
        } else {
            val path = cache.filename
            if (path != null) {
                FetchedCache.Existed(path)
            } else {
                FetchedCache.Unknown
            }
        }
    }

    fun remove(@CacheDomain domain: Int, id: Long, source: Int): Boolean {
        val dao = database.ImageCacheDao()
        return dao.delete(domain, id, source) > 0
    }

    fun register(
        domain: Int,
        id: Long,
        source: Int,
        filename: String?,
    ): Boolean {
        val dao = database.ImageCacheDao()
        return dao.refresh(
            ImageCacheEntity(
                domain = domain,
                id = id,
                type = source,
                timestamp = System.currentTimeMillis(),
                empty = filename == null,
                filename = filename,
            )
        ) > 0
    }

    fun clear() {
        database.ImageCacheDao().deleteAll()
    }

    private const val TIME_OUT: Long = 7 * (24 * 60 * 60 * 1000)
}
