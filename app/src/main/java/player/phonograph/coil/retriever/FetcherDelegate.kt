/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.retriever

import coil.fetch.FetchResult
import coil.size.Size
import player.phonograph.coil.model.AlbumImage
import player.phonograph.coil.model.ArtistImage
import player.phonograph.coil.model.SongImage
import player.phonograph.util.debug
import android.content.Context
import android.util.Log

sealed class FetcherDelegate<T, R : ImageRetriever> {

    // private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    abstract val retriever: R

    abstract val cacheStore: CacheStore.Cache<T>

    private fun isNoImage(target: T): Boolean = cacheStore.isNoImage(target, retriever.id)
    private fun markNoImage(target: T) = cacheStore.markNoImage(target, retriever.id)
    private fun getCache(target: T): FetchResult? = cacheStore.get(target, retriever.id)
    private fun setCache(target: T, result: FetchResult) = cacheStore.set(target, result, retriever.id)

    fun retrieve(
        target: T,
        context: Context,
        size: Size,
        rawImage: Boolean,
    ): FetchResult? {

        /*
        val noSpecificImage = isNoImage(target)
        if (noSpecificImage) return null
        */

        /*
        val cached = getCache(target)
        if (cached != null) {
            debug {
                Log.v(TAG, "Image was read from cache of ${retriever.name} for file $target")
            }
            return cached
        }
        */

        val result = retrieveImpl(target, context, size, rawImage)
        return if (result != null) {
            /*
            coroutineScope.launch {
                setCache(target, result)
            }
            */
            result
        } else {
            debug {
                Log.v(TAG, "Image not available from ${retriever.name} for $target")
            }
            /*
            markNoImage(target)
            */
            null
        }
    }

    abstract fun retrieveImpl(
        target: T,
        context: Context,
        size: Size,
        rawImage: Boolean,
    ): FetchResult?

    companion object {
        private const val TAG = "FetcherDelegate"
    }
}


class AudioFileImageFetcherDelegate<R : ImageRetriever>(
    context: Context,
    override val retriever: R,
) : FetcherDelegate<SongImage, R>() {

    override val cacheStore: CacheStore.Cache<SongImage> = CacheStore.AudioFiles(context.applicationContext)

    override fun retrieveImpl(target: SongImage, context: Context, size: Size, rawImage: Boolean): FetchResult? {
        return retriever.retrieve(target.path, target.albumId, context, size, rawImage)
    }
}

sealed class CompositeFetcherDelegate<T, R : ImageRetriever>(
    override val retriever: R,
) : FetcherDelegate<T, R>() {

    abstract fun iteratorDelegate(target: T): Collection<SongImage>

    override fun retrieveImpl(target: T, context: Context, size: Size, rawImage: Boolean): FetchResult? {
        // val audioFilesCache = CacheStore.AudioFiles(context.applicationContext)
        for (file in iteratorDelegate(target)) {
            /*
            val noImage = audioFilesCache.isNoImage(file)
            if (noImage) continue
            */
            val result = retriever.retrieve(file.path, file.albumId, context, size, rawImage)
            if (result != null) {
                return result
            } else {
                continue
            }
        }
        return null
    }
}

class AlbumImageFetcherDelegate<R : ImageRetriever>(
    context: Context,
    retriever: R,
) : CompositeFetcherDelegate<AlbumImage, R>(retriever) {
    override val cacheStore: CacheStore.Cache<AlbumImage> = CacheStore.AlbumImages(context.applicationContext)
    override fun iteratorDelegate(target: AlbumImage): Collection<SongImage> = target.files
}

class ArtistImageFetcherDelegate<R : ImageRetriever>(
    context: Context,
    retriever: R,
) : CompositeFetcherDelegate<ArtistImage, R>(retriever) {
    override val cacheStore: CacheStore.Cache<ArtistImage> = CacheStore.ArtistImages(context.applicationContext)
    override fun iteratorDelegate(target: ArtistImage): Collection<SongImage> = target.files
}

