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
import player.phonograph.coil.audiofile.retrieveAudioFile
import player.phonograph.coil.retriever.CacheStore
import player.phonograph.coil.retriever.ExternalFileRetriever
import player.phonograph.coil.retriever.ImageRetriever
import player.phonograph.coil.retriever.raw
import player.phonograph.coil.retriever.readFromFile
import player.phonograph.coil.retriever.retrieverFromConfig
import player.phonograph.util.debug
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArtistImageFetcher(
    val data: ArtistImage,
    val context: Context,
    val size: Size,
    private val raw: Boolean,
) : Fetcher {

    class Factory : Fetcher.Factory<ArtistImage> {
        override fun create(
            data: ArtistImage,
            options: Options,
            imageLoader: ImageLoader,
        ) =
            ArtistImageFetcher(data, options.context, options.size, options.raw(false))
    }

    override suspend fun fetch(): FetchResult? {
        // first check if the custom artist image exist
        val file = CustomArtistImageStore.instance(context).getCustomArtistImageFile(data.id, data.name)
        if (file != null) {
            return readFromFile(file, "#${data.id}#${data.name}", "image/jpeg")
        }
        // then choose an AlbumCover as ArtistImage
        return retrieve(retriever, data, context, size)
    }

    private fun retrieve(
        retrievers: List<ImageRetriever>,
        artist: ArtistImage,
        context: Context,
        size: Size,
    ): FetchResult? {
        val noImage = CacheStore.ArtistImages(context).isNoImage(artist)
        if (noImage) return null // skipping

        for (file in artist.files) {
            val cached = CacheStore.ArtistImages(context).get(artist, file.songId.toString())
            if (cached != null) {
                debug {
                    Log.v(TAG, "Image was read from cache of file($file) for artist $artist")
                }
                return cached
            }
            val noSpecificImage = CacheStore.ArtistImages(context).isNoImage(artist, file.songId.toString())
            if (noSpecificImage) continue
            val result = retrieveAudioFile(retrievers, file, context, size, raw)
            if (result != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    CacheStore.ArtistImages(context).set(artist, result, file.songId.toString())
                }
                return result
            } else {
                CacheStore.ArtistImages(context).markNoImage(artist, file.songId.toString())
            }
        }
        debug {
            Log.v(TAG, "No any image for artist ${artist.name}")
        }
        CacheStore.ArtistImages(context).markNoImage(artist)
        return null
    }

    companion object {
        val retriever = retrieverFromConfig.filter { it !is ExternalFileRetriever }
        // ExternalFileRetriever is not suitable for artist
        private const val TAG = "ArtistImageFetcher"
    }
}
