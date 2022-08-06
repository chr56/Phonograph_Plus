/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.audiofile

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.ContentMetadata
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.Dimension
import coil.size.Size
import java.io.File
import java.io.InputStream
import okio.buffer
import okio.source
import org.jaudiotagger.audio.mp3.MP3File
import player.phonograph.BuildConfig
import player.phonograph.coil.IgnoreMediaStorePreference
import player.phonograph.util.MusicUtil

class AudioFileFetcher private constructor(
    val audioFile: AudioFile,
    val context: Context,
    val size: Size
) : Fetcher {

    class Factory : Fetcher.Factory<AudioFile> {
        override fun create(data: AudioFile, options: Options, imageLoader: ImageLoader): Fetcher {
            return AudioFileFetcher(data, options.context, options.size)
        }
    }

    override suspend fun fetch(): FetchResult? {
        // returning bitmap
        var bitmap: Bitmap?

        //
        // 0: lookup for MediaStore
        //
        if (!IgnoreMediaStorePreference.ignoreMediaStore) {
            val uri = MusicUtil.getMediaStoreAlbumCoverUri(audioFile.albumId)
            runCatching {
                val result = readFromMediaStore(uri)
                if (result != null) return result
            }
        }

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
                    Log.v(TAG, "No cover for $audioFile from Android naive MediaMetadataRetriever")
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
                        mp3File.tag.firstArtwork?.binaryData?.toBitmap() ?: return@runCatching null
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

    @OptIn(ExperimentalCoilApi::class)
    private fun readFromMediaStore(uri: Uri): SourceResult? {
        val contentResolver = context.contentResolver
        val inputStream: InputStream? =
            if (Build.VERSION.SDK_INT >= 29) {
                val bundle: Bundle? =
                    run {
                        val width = (size.width as? Dimension.Pixels)?.px ?: return@run null
                        val height = (size.height as? Dimension.Pixels)?.px ?: return@run null
                        Bundle(1).apply {
                            putParcelable(
                                ContentResolver.EXTRA_SIZE,
                                Point(width, height)
                            )
                        }
                    }
                contentResolver.openTypedAssetFile(uri, "image/*", bundle, null)?.createInputStream()
            } else {
                contentResolver.openInputStream(uri)
            }
        return if (inputStream != null) SourceResult(
            source = ImageSource(
                source = inputStream.source().buffer(),
                context = context,
                metadata = ContentMetadata(uri)
            ),
            mimeType = contentResolver.getType(uri),
            dataSource = DataSource.DISK
        ) else null
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
