/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.audiofile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension
import coil.size.Size
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.coil.*

class AudioFileFetcher private constructor(
    private val audioFile: AudioFile,
    private val context: Context,
    private val size: Size,
) : Fetcher {

    class Factory : Fetcher.Factory<AudioFile> {
        override fun create(data: AudioFile, options: Options, imageLoader: ImageLoader): Fetcher {
            return AudioFileFetcher(data, options.context, options.size)
        }
    }

    override suspend fun fetch(): FetchResult? {
        // returning bitmap
        var bitmap: Bitmap?

        val width = (size.width as? Dimension.Pixels)?.px ?: -1
        val height = (size.height as? Dimension.Pixels)?.px ?: -1

        //
        // 0: lookup for MediaStore
        //
        if (!IgnoreMediaStorePreference.ignoreMediaStore) {
            val result = readFromMediaStore(audioFile.albumId, context, size)
            if (result != null) {
                return result
            } else {
                if (DEBUG) {
                    Log.v(TAG, "No cover for $audioFile from Android MediaStore")
                }
            }
        }

        //
        // 1: From Android MediaMetadataRetriever
        //
        MediaMetadataRetriever().use { retriever ->
            bitmap = retrieveFromMediaMetadataRetriever(audioFile.path, retriever, width, height)
            if (DEBUG && bitmap == null) {
                Log.v(TAG, "No cover for $audioFile from Android naive MediaMetadataRetriever")
            }
        }

        //
        // 2: Use JAudioTagger to get embedded high resolution album art if there is any
        //
        if (bitmap == null) {
            bitmap = retrieveFromJAudioTagger(audioFile.path, width, height)
            if (DEBUG && bitmap == null) {
                Log.v(TAG, "No cover for $audioFile in tags")
            }
        }

        //
        // 3: Look for album art in external files
        //
        if (bitmap == null) {
            bitmap = retrieveFromExternalFile(audioFile.path, width, height)
            if (DEBUG && bitmap == null) {
                Log.v(TAG, "No cover file along with $audioFile in folder")
            }
        }

        //
        // Collect Result
        //
        return if (bitmap != null) {
            DrawableResult(
                BitmapDrawable(context.resources, bitmap),
                false,
                DataSource.DISK
            )
        } else {
            if (DEBUG) {
                Log.v(TAG, "No any cover for $audioFile")
            }
            null
        }
    }

    companion object {
        private const val TAG = "AudioFileFetcher"
    }
}
