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
import player.phonograph.util.debug
import player.phonograph.util.mediaStoreAlbumArtUri
import player.phonograph.util.recordThrowable
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import android.util.Size as AndroidSize



internal fun retrieveFromMediaStore(
    albumId: Long,
    context: Context,
    size: Size,
): SourceResult? =
    runCatching {
        retrieveFromAlbumUri(albumId, context, size)
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
internal fun retrieveFromAlbumUri(
    albumId: Long,
    context: Context,
    size: Size,
): SourceResult? {
    val contentResolver = context.contentResolver
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Albums.getContentUri(MediaStore.VOLUME_EXTERNAL),
            albumId
        )
        val width = size.width.pxOrElse { -1 }
        val height = size.height.pxOrElse { -1 }
        try {
            val bitmap = contentResolver.loadThumbnail(uri, AndroidSize(width, height), null)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream).let { if (!it) throw IOException("Failed!") }
            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            val source = inputStream.source().buffer()
            SourceResult(
                source = ImageSource(
                    source = source,
                    context = context,
                    metadata = ContentMetadata(uri)
                ),
                mimeType = "image/png",
                dataSource = DataSource.DISK
            )
        } catch (e: IOException) {
            if (e is FileNotFoundException) {
                debug { Log.v("loadThumbnail", "File not available ($uri)!") }
            } else {
                recordThrowable(context, "loadThumbnail", e)
            }
            null
        }
    } else {
        val uri = mediaStoreAlbumArtUri(albumId)
        @SuppressLint("Recycle")
        val source = contentResolver.openInputStream(uri)?.source()?.buffer()
        if (source != null)
            SourceResult(
                source = ImageSource(
                    source = source,
                    context = context,
                    metadata = ContentMetadata(uri)
                ),
                mimeType = contentResolver.getType(uri),
                dataSource = DataSource.DISK
            )
        else
            null
    }
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