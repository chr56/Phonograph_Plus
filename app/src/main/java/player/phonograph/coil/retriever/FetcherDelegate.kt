/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.retriever

import coil.fetch.FetchResult
import coil.size.Size
import player.phonograph.coil.model.AlbumImage
import player.phonograph.coil.model.ArtistImage
import player.phonograph.coil.model.CompositeLoaderTarget
import player.phonograph.coil.model.LoaderTarget
import player.phonograph.coil.model.SongImage
import player.phonograph.mechanism.setting.CoilImageConfig
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.debug
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


sealed class FetcherDelegate<T : LoaderTarget, R : ImageRetriever> {

    @Suppress("UNUSED_PARAMETER")
    protected fun enableCache(context: Context): Boolean = CoilImageConfig.enableImageCache

    abstract val retriever: R

    abstract val cacheStore: CacheStore.Cache<T>

    fun retrieve(
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
                CacherCoroutineScope.launch {
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

sealed class CompositeFetcherDelegate<T : CompositeLoaderTarget<SongImage>, R : ImageRetriever>(
    override val retriever: R,
) : FetcherDelegate<T, R>() {

    override fun retrieveImpl(target: T, context: Context, size: Size, rawImage: Boolean): FetchResult? {

        if (enableCache(context)) {
            val noImage = cacheStore.isNoImage(target, retriever.id)
            if (noImage) return null

            val cached = cacheStore.get(target, retriever.id)
            if (cached != null) {
                return cached
            }
        }

        val audioFilesCache = CacheStore.AudioFiles(context.applicationContext)

        for (file in target.disassemble()) {

            if (enableCache(context)) {
                val noSpecificImage = audioFilesCache.isNoImage(file, retriever.id)
                if (noSpecificImage) continue
            }

            val result = retriever.retrieve(file.path, file.albumId, context, size, rawImage)
            if (result != null) {

                if (enableCache(context)) {
                    CacherCoroutineScope.launch {
                        cacheStore.set(target, result, retriever.id)
                    }
                }

                return result
            } else {
                continue
            }
        }

        if (enableCache(context)) {
            cacheStore.markNoImage(target, retriever.id)
        }

        return null
    }
}

class AlbumImageFetcherDelegate<R : ImageRetriever>(
    context: Context,
    retriever: R,
) : CompositeFetcherDelegate<AlbumImage, R>(retriever) {
    override val cacheStore: CacheStore.Cache<AlbumImage> = CacheStore.AlbumImages(context.applicationContext)
}

class ArtistImageFetcherDelegate<R : ImageRetriever>(
    context: Context,
    retriever: R,
) : CompositeFetcherDelegate<ArtistImage, R>(retriever) {
    override val cacheStore: CacheStore.Cache<ArtistImage> = CacheStore.ArtistImages(context.applicationContext)
}



private val CacherCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)