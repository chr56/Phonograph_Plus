/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.audiofile

import coil.ImageLoader
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Size
import player.phonograph.coil.ExternalFileRetriever
import player.phonograph.coil.IgnoreMediaStorePreference
import player.phonograph.coil.ImageRetriever
import player.phonograph.coil.JAudioTaggerRetriever
import player.phonograph.coil.MediaMetadataRetriever
import player.phonograph.coil.MediaStoreRetriever
import player.phonograph.util.Util.debug
import android.content.Context
import android.util.Log

class AudioFileFetcher private constructor(
    private val audioFile: AudioFile,
    private val context: Context,
    private val size: Size,
) : Fetcher {

    class Factory : Fetcher.Factory<AudioFile> {
        override fun create(data: AudioFile, options: Options, imageLoader: ImageLoader): Fetcher =
            AudioFileFetcher(data, options.context, options.size)
    }

    override suspend fun fetch(): FetchResult? =
        retrieve(retriever, audioFile, context, size)

    private fun retrieve(
        retrievers: List<ImageRetriever>,
        audioFile: AudioFile,
        context: Context,
        size: Size
    ): FetchResult? {
        for (retriever in retrievers) {
            val result = retriever.retrieve(audioFile, context, size)
            if (result == null) {
                debug {
                    Log.v(TAG, "Image not available from ${retriever.name} for $audioFile")
                }
                continue
            } else {
                return result
            }
        }
        debug {
            Log.v(TAG, "No any cover for $audioFile")
        }
        return null
    }

    companion object {
        val retriever =
            if (!IgnoreMediaStorePreference.ignoreMediaStore) listOf(
                MediaStoreRetriever(),
                MediaMetadataRetriever(),
                JAudioTaggerRetriever(),
                ExternalFileRetriever()
            ) else listOf(
                MediaMetadataRetriever(),
                JAudioTaggerRetriever(),
                ExternalFileRetriever()
            )
        private const val TAG = "ImageRetriever"
    }
}
