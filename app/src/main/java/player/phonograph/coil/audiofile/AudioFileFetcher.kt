/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.audiofile

import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Size
import player.phonograph.coil.retriever.ImageRetriever
import player.phonograph.coil.retriever.raw
import player.phonograph.coil.retriever.retrieveAudioFile
import player.phonograph.coil.retriever.retrieverFromConfig
import android.content.Context

class AudioFileFetcher private constructor(
    private val audioFile: AudioFile,
    private val context: Context,
    private val size: Size,
    private val rawImage: Boolean,
) : Fetcher {

    class Factory : Fetcher.Factory<AudioFile> {
        override fun create(data: AudioFile, options: Options, imageLoader: ImageLoader): Fetcher =
            AudioFileFetcher(
                data,
                options.context,
                options.size,
                options.raw(false),
            )
    }

    override suspend fun fetch(): FetchResult? =
        retrieve(retriever, audioFile, context, size)

    private fun retrieve(
        retrievers: List<ImageRetriever>,
        audioFile: AudioFile,
        context: Context,
        size: Size,
    ): FetchResult? {
        return retrieveAudioFile(retrievers, audioFile, context, size, rawImage)
    }

    companion object {
        val retriever = retrieverFromConfig
        private const val TAG = "ImageRetriever"
    }
}
