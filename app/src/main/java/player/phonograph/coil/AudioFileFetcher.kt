/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import java.io.File
import org.jaudiotagger.audio.mp3.MP3File
import player.phonograph.BuildConfig

class AudioFileFetcher private constructor(val audioFile: AudioFile, val context: Context) : Fetcher {

    class Factory : Fetcher.Factory<AudioFile> {
        override fun create(data: AudioFile, options: Options, imageLoader: ImageLoader): Fetcher {
            return AudioFileFetcher(data, options.context)
        }
    }

    override suspend fun fetch(): FetchResult? {
        // returning bitmap
        var bitmap: Bitmap?

        //
        // 1: From Android MediaMetadataRetriever
        //
        val retriever = MediaMetadataRetriever()
        val embeddedPicture: ByteArray? =
            try {
                retriever.setDataSource(audioFile.path)
                retriever.embeddedPicture
            } catch (ignored: Exception) {
                null
            }
        bitmap =
            if (embeddedPicture != null) {
                // success
                embeddedPicture.toBitmap()
            } else {
                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "No cover for $audioFile from MediaStore")
                }
                null
            }

        //
        // 2: Use JAudioTagger to get embedded high resolution album art if there is any
        //
        if (bitmap == null) {
            bitmap =
                runCatching {
                    val mp3File = MP3File(audioFile.path)
                    /* return@runCatching */ if (mp3File.hasID3v2Tag()) {
                        mp3File.tag.firstArtwork?.binaryData?.toBitmap() ?: return null
                    } else {
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "No tag containing imagine for $audioFile")
                        }
                        null
                    }
                }.getOrNull()
        }

        //
        // 3: Look for album art in external files
        //
        if (bitmap == null) {
            val parent = File(audioFile.path).parentFile ?: return null
            for (fallback in fallbackFiles) {
                val coverFile = File(parent, fallback)
                bitmap = if (coverFile.exists()) {
                    BitmapFactory.decodeFile(coverFile.absolutePath)
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "No cover file along with $audioFile in folder")
                    }
                    null
                }
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
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "No any cover for $audioFile")
            }
            null
        }
    }

    companion object {
        private const val TAG = "AudioFileFetcher"
        val fallbackFiles = arrayOf(
            "cover.jpg",
            "album.jpg",
            "folder.jpg",
            "cover.png",
            "album.png",
            "folder.png"
        )

        fun ByteArray.toBitmap(): Bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)
    }
}
