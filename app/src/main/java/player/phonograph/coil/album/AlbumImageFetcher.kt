/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.album

import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Size
import player.phonograph.coil.retriever.ImageRetriever
import player.phonograph.coil.retriever.raw
import player.phonograph.coil.retriever.retrieverFromConfig
import player.phonograph.util.debug
import android.content.Context
import android.util.Log

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
        data: AlbumImage,
        context: Context,
        size: Size,
    ): FetchResult? {
        for (file in data.files) {
            for (retriever in retrievers) {
                val result = retriever.retrieve(file.path, file.songId, context, size, raw)
                if (result == null) {
                    debug {
                        Log.v(
                            TAG,
                            "Image not available from ${retriever.name} for ${data.name} in file ${file.songId}"
                        )
                    }
                    continue
                } else {
                    return result
                }
            }
        }
        debug {
            Log.v(TAG, "No any cover for ${data.name}")
        }
        return null
    }

    companion object {
        val retriever = retrieverFromConfig
        private const val TAG = "AlbumImageFetcher"
    }
}
