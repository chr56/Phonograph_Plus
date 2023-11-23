/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.artist

import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Size
import player.phonograph.coil.CustomArtistImageStore
import player.phonograph.coil.retriever.ArtistImageFetcherDelegate
import player.phonograph.coil.retriever.ExternalFileRetriever
import player.phonograph.coil.retriever.ImageRetriever
import player.phonograph.coil.retriever.raw
import player.phonograph.coil.retriever.readFromFile
import player.phonograph.coil.retriever.retrieverFromConfig
import player.phonograph.util.debug
import android.content.Context
import android.util.Log

class ArtistImageFetcher(
    private val data: ArtistImage,
    private val context: Context,
    private val size: Size,
    private val raw: Boolean,
    private val delegates: List<ArtistImageFetcherDelegate<ImageRetriever>>,
) : Fetcher {

    class Factory(context: Context) : Fetcher.Factory<ArtistImage> {
        override fun create(
            data: ArtistImage,
            options: Options,
            imageLoader: ImageLoader,
        ) =
            ArtistImageFetcher(data, options.context, options.size, options.raw(false), delegates)

        private val delegates: List<ArtistImageFetcherDelegate<ImageRetriever>> =
            retrieverFromConfig
                .filter { it !is ExternalFileRetriever }  // ExternalFileRetriever is not suitable for artist
                .map { ArtistImageFetcherDelegate(context.applicationContext, it) }
    }

    override suspend fun fetch(): FetchResult? {
        // first check if the custom artist image exist
        val file = CustomArtistImageStore.instance(context).getCustomArtistImageFile(data.id, data.name)
        if (file != null) {
            return readFromFile(file, "#${data.id}#${data.name}", "image/jpeg")
        }
        // then try to receive from delegates
        /*
        val noImage = CacheStore.ArtistImages(context).isNoImage(data)
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
            Log.v(TAG, "No any cover for artist $data")
        }
        /*
        CacheStore.ArtistImages(context).markNoImage(data)
        */
        return null
    }

    companion object {
        private const val TAG = "ArtistImageFetcher"
    }
}
