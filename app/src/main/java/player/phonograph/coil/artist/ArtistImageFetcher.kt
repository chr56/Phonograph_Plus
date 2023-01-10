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
import player.phonograph.coil.IgnoreMediaStorePreference
import player.phonograph.coil.retriever.ExternalFileRetriever
import player.phonograph.coil.retriever.ImageRetriever
import player.phonograph.coil.retriever.JAudioTaggerRetriever
import player.phonograph.coil.retriever.MediaMetadataRetriever
import player.phonograph.coil.retriever.MediaStoreRetriever
import player.phonograph.coil.retriever.readFromFile
import player.phonograph.coil.retriever.retrieverFromConfig
import player.phonograph.util.Util.debug
import android.content.Context
import android.util.Log

class ArtistImageFetcher(val data: ArtistImage, val context: Context, val size: Size) : Fetcher {

    class Factory : Fetcher.Factory<ArtistImage> {
        override fun create(
            data: ArtistImage,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            return ArtistImageFetcher(data, options.context, options.size)
        }
    }

    override suspend fun fetch(): FetchResult? {
        // first check if the custom artist image exist
        val file = CustomArtistImageStore.instance(context)
            .getCustomArtistImageFile(data.artistId, data.artistName)
        if (file != null) {
            return readFromFile(file, "#${data.artistId}#${data.artistName}", "image/jpeg")
        }
        // then choose an AlbumCover as ArtistImage
        return retrieve(retriever, data, context, size)
    }

    private fun retrieve(
        retrievers: List<ImageRetriever>,
        data: ArtistImage,
        context: Context,
        size: Size
    ): FetchResult? {
        for (cover in data.albumCovers) {
            for (retriever in retrievers) {
                val result = retriever.retrieve(cover.filePath, cover.id, context, size)
                if (result == null) {
                    debug {
                        Log.v(
                            TAG,
                            "Image not available from ${retriever.name} for ${data.artistName} in cover ${cover.id}"
                        )
                    }
                    continue
                } else {
                    return result
                }
            }
        }
        debug {
            Log.v(TAG, "No any cover for ${data.artistName}")
        }
        return null
    }

    companion object {
        val retriever = retrieverFromConfig.filter { it !is ExternalFileRetriever }
        // ExternalFileRetriever is not suitable for artist
        private const val TAG = "ArtistImageFetcher"
    }
}
