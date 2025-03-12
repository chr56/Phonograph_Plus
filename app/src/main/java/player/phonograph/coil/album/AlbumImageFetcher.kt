/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.album

import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Size
import player.phonograph.coil.model.AlbumImage
import player.phonograph.coil.raw
import player.phonograph.coil.retriever.AlbumImageFetcherDelegate
import player.phonograph.coil.retriever.ImageRetriever
import player.phonograph.coil.retriever.retrievers
import player.phonograph.util.debug
import android.content.Context
import android.util.Log

class AlbumImageFetcher(
    private val data: AlbumImage,
    private val context: Context,
    private val size: Size,
    private val raw: Boolean,
    private val delegates: List<AlbumImageFetcherDelegate<ImageRetriever>>,
) : Fetcher {

    class Factory() : Fetcher.Factory<AlbumImage> {
        override fun create(
            data: AlbumImage,
            options: Options,
            imageLoader: ImageLoader,
        ) = AlbumImageFetcher(
            data,
            options.context,
            options.size,
            options.parameters.raw(false),
            options.parameters.retrievers().map {
                AlbumImageFetcherDelegate(options.context, it)
            }
        )
    }

    override suspend fun fetch(): FetchResult? {
        /*
        val noImage = CacheStore.AlbumImages(context).isNoImage(data)
        if (noImage) return null // skipping
        */
        for (delegate in delegates) {
            val result = delegate.retrieve(data, context, size, raw)
            if (result != null) {
                return result
            } else {
                continue
            }
        }
        debug {
            Log.v(TAG, "No any cover for album $data")
        }
        /*
        CacheStore.AlbumImages(context).markNoImage(data)
        */
        return null
    }

    companion object {
        private const val TAG = "AlbumImageFetcher"
    }
}
