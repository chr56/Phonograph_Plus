/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.audiofile

import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Size
import player.phonograph.coil.model.SongImage
import player.phonograph.coil.retriever.AudioFileImageFetcherDelegate
import player.phonograph.coil.retriever.ImageRetriever
import player.phonograph.coil.retriever.raw
import player.phonograph.coil.retriever.retrieverFromConfig
import player.phonograph.util.debug
import android.content.Context
import android.util.Log

class AudioFileFetcher private constructor(
    private val songImage: SongImage,
    private val context: Context,
    private val size: Size,
    private val rawImage: Boolean,
    private val delegates: List<AudioFileImageFetcherDelegate<ImageRetriever>>,
) : Fetcher {

    class Factory(context: Context) : Fetcher.Factory<SongImage> {
        override fun create(data: SongImage, options: Options, imageLoader: ImageLoader): Fetcher =
            AudioFileFetcher(
                data,
                options.context,
                options.size,
                options.raw(false),
                delegates
            )

        private val delegates: List<AudioFileImageFetcherDelegate<ImageRetriever>> =
            retrieverFromConfig.map { AudioFileImageFetcherDelegate(context.applicationContext, it) }
    }

    override suspend fun fetch(): FetchResult? {
        /*
        val noImage = CacheStore.AudioFiles(context).isNoImage(audioFile)
        if (noImage) return null // skipping
         */
        for (delegate in delegates) {
            val result = delegate.retrieve(songImage, context, size, rawImage)
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
        val retriever = retrieverFromConfig
        private const val TAG = "ImageRetriever"
    }
}
