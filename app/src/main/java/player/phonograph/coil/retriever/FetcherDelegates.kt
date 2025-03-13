/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.retriever

import coil.fetch.FetchResult
import coil.size.Size
import player.phonograph.coil.cache.CacheStore
import player.phonograph.coil.model.AlbumImage
import player.phonograph.coil.model.ArtistImage
import player.phonograph.coil.model.CompositeLoaderTarget
import player.phonograph.coil.model.SongImage
import android.content.Context
import kotlinx.coroutines.launch


class AudioFileImageFetcherDelegate<R : ImageRetriever>(
    context: Context,
    override val retriever: R,
) : FetcherDelegate<SongImage, R>() {

    override val cacheStore: CacheStore.Cache<SongImage> = CacheStore.AudioFiles(context.applicationContext)

    override suspend fun retrieveImpl(target: SongImage, context: Context, size: Size, rawImage: Boolean): FetchResult? {
        return retriever.retrieve(target.path, target.albumId, context, size, rawImage)
    }
}

sealed class CompositeFetcherDelegate<T : CompositeLoaderTarget<SongImage>, R : ImageRetriever>(
    override val retriever: R,
) : FetcherDelegate<T, R>() {

    override suspend fun retrieveImpl(target: T, context: Context, size: Size, rawImage: Boolean): FetchResult? {

        val audioFilesCache = CacheStore.AudioFiles(context.applicationContext)

        for (file in target.items(context)) {

            if (enableCache(context)) {
                val noSpecificImage = audioFilesCache.isNoImage(file, retriever.id)
                if (noSpecificImage) continue
            }

            val result = retriever.retrieve(file.path, file.albumId, context, size, rawImage)
            if (result != null) return result else continue
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