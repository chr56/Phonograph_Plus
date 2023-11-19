/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.album

import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Size
import player.phonograph.coil.audiofile.retrieveAudioFile
import player.phonograph.coil.retriever.CacheStore
import player.phonograph.coil.retriever.ImageRetriever
import player.phonograph.coil.retriever.raw
import player.phonograph.coil.retriever.retrieverFromConfig
import player.phonograph.util.debug
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlbumImageFetcher(
    val data: AlbumImage,
    val context: Context,
    val size: Size,
    private val raw: Boolean,
) : Fetcher {

    class Factory : Fetcher.Factory<AlbumImage> {
        override fun create(
            data: AlbumImage,
            options: Options,
            imageLoader: ImageLoader,
        ) =
            AlbumImageFetcher(data, options.context, options.size, options.raw(false))
    }

    override suspend fun fetch(): FetchResult? =
        retrieve(retriever, data, context, size)

    private fun retrieve(
        retrievers: List<ImageRetriever>,
        album: AlbumImage,
        context: Context,
        size: Size,
    ): FetchResult? {
        val noImage = CacheStore.AlbumImages(context).isNoImage(album)
        if (noImage) return null // skipping

        for (file in album.files) {
            val cached = CacheStore.AlbumImages(context).get(album, file.songId.toString())
            if (cached != null) {
                debug {
                    Log.v(TAG, "Image was read from cache of file($file) for album $album")
                }
                return cached
            }
            val noSpecificImage = CacheStore.AlbumImages(context).isNoImage(album, file.songId.toString())
            if (noSpecificImage) continue
            val result = retrieveAudioFile(retrievers, file, context, size, raw)
            if (result != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    CacheStore.AlbumImages(context).set(album, result, file.songId.toString())
                }
                return result
            } else {
                CacheStore.AlbumImages(context).markNoImage(album, file.songId.toString())
            }
        }
        debug {
            Log.v(TAG, "No any image for album ${album.name}")
        }
        CacheStore.AlbumImages(context).markNoImage(album)
        return null
    }

    companion object {
        val retriever = retrieverFromConfig
        private const val TAG = "AlbumImageFetcher"
    }
}
