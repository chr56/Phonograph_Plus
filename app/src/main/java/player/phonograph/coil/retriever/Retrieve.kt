/*
 * Copyright (c) 2022~2023 chr_56
 */

@file:JvmName("RetrieveImages")

package player.phonograph.coil.retriever

import coil.annotation.ExperimentalCoilApi
import coil.decode.ContentMetadata
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.SourceResult
import coil.size.Size
import coil.size.pxOrElse
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import java.io.File

internal fun readFromMediaStore(
    albumId: Long,
    context: Context,
    size: Size,
): SourceResult? =
    runCatching {
        val uri = getMediaStoreAlbumCoverUri(albumId)
        readFromMediaStore(uri, context, size)
    }.getOrNull()

internal fun retrieveFromMediaMetadataRetriever(
    filepath: String,
    retriever: MediaMetadataRetriever,
    size: Size,
    raw: Boolean,
): Bitmap? {
    val embeddedPicture: ByteArray? =
        runCatching {
            retriever.setDataSource(filepath)
            retriever.embeddedPicture
        }.getOrNull()
    return if (raw) {
        embeddedPicture?.toBitmap()
    } else {
        embeddedPicture?.toBitmap(size)
    }
}

internal fun retrieveFromJAudioTagger(
    filepath: String,
    size: Size,
    raw: Boolean,
): Bitmap? = runCatching {
    AudioFileIO.read(File(filepath)).retrieveEmbedPicture(size, raw)
}.getOrNull()

internal fun retrieveFromExternalFile(
    filepath: String,
): FetchResult? {
    val parent = File(filepath).parentFile ?: return null
    for (fallback in folderCoverFiles) {
        val coverFile = File(parent, fallback)
        return if (coverFile.exists()) {
            SourceResult(
                source = ImageSource(
                    file = coverFile.toOkioPath(true),
                    diskCacheKey = filepath
                ),
                mimeType = null,
                dataSource = DataSource.DISK
            )
        } else {
            continue
        }
    }
    return null
}

@OptIn(ExperimentalCoilApi::class)
internal fun readFromMediaStore(
    uri: Uri,
    context: Context,
    size: Size,
): SourceResult? {
    val contentResolver = context.contentResolver
    val source =
        if (Build.VERSION.SDK_INT >= 29) {
            val width = size.width.pxOrElse { -1 }
            val height = size.height.pxOrElse { -1 }
            val bundle: Bundle? =
                if (width >= 0 && height >= 0) {
                    Bundle(1).apply {
                        putParcelable(
                            ContentResolver.EXTRA_SIZE, Point(width, height)
                        )
                    }
                } else null
            contentResolver.openTypedAssetFile(uri, "image/*", bundle, null)?.use {
                it.createInputStream().use { inputStream ->
                    inputStream.source().buffer()
                }
            }
        } else {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.source().buffer()
            }
        }
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

internal fun readFromFile(
    file: File,
    diskCacheKey: String? = null,
    mimeType: String?,
): SourceResult {
    return SourceResult(
        source = ImageSource(
            file = file.toOkioPath(true),
            diskCacheKey = diskCacheKey
        ),
        mimeType = mimeType,
        dataSource = DataSource.DISK
    )
}

internal fun AudioFile.retrieveEmbedPicture(size: Size, raw: Boolean): Bitmap? {
    val artwork = this.tag.firstArtwork
    return artwork?.binaryData?.let { if (raw) it.toBitmap() else it.toBitmap(size) }
}