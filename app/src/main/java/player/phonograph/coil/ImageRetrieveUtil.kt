/*
 * Copyright (c) 2022 chr_56
 */
@file:JvmName("ImageRetrieveUtil")

package player.phonograph.coil

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import coil.annotation.ExperimentalCoilApi
import coil.decode.ContentMetadata
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.size.Dimension
import coil.size.Size
import java.io.File
import java.io.InputStream
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import org.jaudiotagger.audio.mp3.MP3File
import player.phonograph.util.MusicUtil.getMediaStoreAlbumCoverUri

internal fun readFromMediaStore(albumId: Long, context: Context, size: Size): SourceResult? {
    return runCatching {
        val uri = getMediaStoreAlbumCoverUri(albumId)
        readFromMediaStore(uri, context, size)
    }.getOrNull()
}

internal fun retrieveFromMediaMetadataRetriever(filepath: String, retriever: MediaMetadataRetriever): Bitmap? {
    val embeddedPicture: ByteArray? =
        runCatching {
            retriever.setDataSource(filepath)
            retriever.embeddedPicture
        }.getOrNull()
    return embeddedPicture?.toBitmap()
}

internal fun retrieveFromJAudioTagger(filepath: String): Bitmap? {
    return runCatching {
        val mp3File = MP3File(filepath)
        /* return@runCatching */ if (mp3File.hasID3v2Tag()) {
            mp3File.tag.firstArtwork?.binaryData?.toBitmap() ?: return@runCatching null
        } else {
            null
        }
    }.getOrNull()
}

internal fun retrieveFromExternalFile(filepath: String): Bitmap? {
    val parent = File(filepath).parentFile ?: return null
    for (fallback in fallbackCoverFiles) {
        val coverFile = File(parent, fallback)
        return if (coverFile.exists()) {
            BitmapFactory.decodeFile(coverFile.absolutePath)
        } else {
            continue
        }
    }
    return null
}

internal val fallbackCoverFiles = arrayOf(
    "cover.jpg",
    "album.jpg",
    "folder.jpg",
    "cover.png",
    "album.png",
    "folder.png"
)

@OptIn(ExperimentalCoilApi::class)
fun readFromMediaStore(uri: Uri, context: Context, size: Size): SourceResult? {
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

internal fun readJEPGFile(file: File, diskCacheKey: String? = null): SourceResult {
    return SourceResult(
        source = ImageSource(
            file = file.toOkioPath(true),
            diskCacheKey = diskCacheKey
        ),
        mimeType = "image/jpeg",
        dataSource = DataSource.DISK
    )
}

fun ByteArray.toBitmap(): Bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)
