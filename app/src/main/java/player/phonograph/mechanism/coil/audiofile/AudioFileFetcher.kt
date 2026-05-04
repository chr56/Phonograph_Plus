/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.coil.audiofile

import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Size
import player.phonograph.mechanism.coil.cache
import player.phonograph.mechanism.coil.model.SongImage
import player.phonograph.mechanism.coil.raw
import player.phonograph.mechanism.coil.retriever.AudioFileImageFetcherDelegate
import player.phonograph.mechanism.coil.retriever.ImageRetriever
import player.phonograph.mechanism.coil.retriever.retrievers
import player.phonograph.util.debug
import android.content.Context
import android.util.Log

class AudioFileFetcher private constructor(
    private val songImage: SongImage,
    private val context: Context,
    private val size: Size,
    private val rawImage: Boolean,
    private val withCache: Boolean,
    private val delegates: List<AudioFileImageFetcherDelegate<ImageRetriever>>,
) : Fetcher {

    class Factory() : Fetcher.Factory<SongImage> {
        override fun create(data: SongImage, options: Options, imageLoader: ImageLoader): Fetcher =
            AudioFileFetcher(
                data,
                options.context,
                options.size,
                options.parameters.raw(false),
                options.parameters.cache(false),
                options.parameters.retrievers().map {
                    AudioFileImageFetcherDelegate(options.context, it)
                }
            )
    }

    override suspend fun fetch(): FetchResult? {
        /*
        val noImage = CacheStore.AudioFiles(context).isNoImage(audioFile)
        if (noImage) return null // skipping
         */
        for (delegate in delegates) {
            val result = delegate.retrieve(songImage, context, size, rawImage, withCache)
            if (result != null) {
                return result
            } else {
                continue
            }
        }
        debug {
            Log.v(TAG, "No any cover for file $songImage")
        }
        /*
        CacheStore.AudioFiles(context).markNoImage(audioFile)
        */
        return null
    }

    companion object {
        private const val TAG = "ImageRetriever"
    }
}
