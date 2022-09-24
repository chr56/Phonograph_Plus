/*
 * Copyright (c) 2022 chr_56
 */
@file:JvmName("ImageRetrieveUtil")

package player.phonograph.coil

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import player.phonograph.util.MusicUtil.getMediaStoreAlbumCoverUri
import java.io.File
import java.io.InputStream

internal fun readFromMediaStore(albumId: Long, context: Context, size: Size): SourceResult? {
    return runCatching {
        val uri = getMediaStoreAlbumCoverUri(albumId)
        readFromMediaStore(uri, context, size)
    }.getOrNull()
}

internal fun retrieveFromMediaMetadataRetriever(
    filepath: String,
    retriever: MediaMetadataRetriever,
    width: Int = -1,
    height: Int = -1,
): Bitmap? {
    val embeddedPicture: ByteArray? =
        runCatching {
            retriever.setDataSource(filepath)
            retriever.embeddedPicture
        }.getOrNull()
    return embeddedPicture?.toBitmap(width, height)
}

internal fun retrieveFromJAudioTagger(
    filepath: String,
    width: Int = -1,
    height: Int = -1,
): Bitmap? = runCatching {
    AudioFileIO.read(File(filepath)).retrieveEmbedPicture(width, height)
}.getOrNull()

internal fun retrieveFromExternalFile(filepath: String, width: Int = -1, height: Int = -1): Bitmap? {
    val parent = File(filepath).parentFile ?: return null
    for (fallback in fallbackCoverFiles) {
        val coverFile = File(parent, fallback)
        return if (coverFile.exists()) {
            if (width > 0 && height > 0)
                BitmapFactory.decodeFile(coverFile.absolutePath,
                    BitmapFactory.Options().apply {
                        outHeight = height
                        outWidth = height
                    })
            else
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
    val source = inputStream?.use { it.source().buffer() }
    return if (source != null) SourceResult(
        source = ImageSource(
            source = source,
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
fun ByteArray.toBitmap(width: Int, height: Int): Bitmap = toBitmap().resize(width, height)

fun Bitmap.resize(width: Int, height: Int): Bitmap =
    when {
        width <= 0 || height <= 0 -> this // not configured
        (this.width > width || this.height > height) -> makeCenterScaled(this, width, height)
        else -> this
    }

private fun makeCenterScaled(source: Bitmap, width: Int, height: Int): Bitmap {
    var bitmap = source
    val matrix = Matrix()

    while (source.width.toFloat() / width > 1.5f || source.height.toFloat() / height > 1.5f) {
        matrix.preScale(0.6667F, 0.6667F, source.width / 2f, source.height / 2f)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, source.width, source.height, matrix, false)
    }

    val x = if (source.width > width) (source.width - width) / 2 else 0
    val y = if (source.height > height) (source.height - height) / 2 else 0
    return Bitmap.createBitmap(bitmap, x, y, width, height)
}


fun AudioFile.retrieveEmbedPicture(width: Int, height: Int): Bitmap? {
    val artwork = this.tag.firstArtwork
    return artwork?.binaryData?.toBitmap(width, height)
}