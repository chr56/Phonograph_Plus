/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.retriever

import coil.annotation.ExperimentalCoilApi
import coil.decode.ContentMetadata
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.SourceResult
import coil.size.Size
import coil.size.pxOrElse
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import player.phonograph.foundation.error.record
import player.phonograph.foundation.mediastore.mediaStoreUriAlbumArt
import player.phonograph.foundation.mediastore.mediastoreUriAlbum
import player.phonograph.mechanism.metadata.JAudioTaggerExtractor
import player.phonograph.util.debug
import androidx.annotation.IntDef
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import android.media.MediaMetadataRetriever as AndroidMediaMetadataRetriever
import android.util.Size as AndroidSize


object ImageRetrievers {

    const val RETRIEVER_ID_MEDIA_STORE = 0
    const val RETRIEVER_ID_MEDIA_METADATA = 1
    const val RETRIEVER_ID_J_AUDIO_TAGGER = 4
    const val RETRIEVER_ID_EXTERNAL_FILE = 7


    @IntDef(
        RETRIEVER_ID_MEDIA_STORE,
        RETRIEVER_ID_MEDIA_METADATA,
        RETRIEVER_ID_J_AUDIO_TAGGER,
        RETRIEVER_ID_EXTERNAL_FILE
    )
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.VALUE_PARAMETER)
    annotation class RetrieverId

    class MediaStoreRetriever : ImageRetriever {
        override val name: String = "MediaStoreRetriever"
        override val id: Int = RETRIEVER_ID_MEDIA_STORE
        override fun retrieve(
            path: String,
            id: Long,
            context: Context,
            size: Size,
            raw: Boolean,
        ): FetchResult? {
            return try {
                retrieveFromAlbumUri(id, context, size)
            } catch (e: Exception) {
                debug {
                    Log.i(name, "${e.javaClass.name}: ${e.message}")
                }
                null
            }
        }

        @OptIn(ExperimentalCoilApi::class)
        private fun retrieveFromAlbumUri(albumId: Long, context: Context, size: Size): FetchResult? {
            val contentResolver = context.contentResolver
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = mediastoreUriAlbum(MediaStore.VOLUME_EXTERNAL, albumId)
                val width = size.width.pxOrElse { -1 }
                val height = size.height.pxOrElse { -1 }
                try {
                    val bitmap = contentResolver.loadThumbnail(uri, AndroidSize(width, height), null)
                    val drawable = BitmapDrawable(context.resources, bitmap)
                    DrawableResult(
                        drawable = drawable,
                        isSampled = false,
                        dataSource = DataSource.DISK
                    )
                } catch (e: IOException) {
                    if (e is FileNotFoundException) {
                        debug { Log.v("loadThumbnail", "File not available ($uri)!") }
                    } else {
                        record(context, e, "loadThumbnail")
                    }
                    null
                }
            } else {
                val uri = mediaStoreUriAlbumArt(albumId)
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

    }

    class MediaMetadataRetriever : ImageRetriever {
        override val name: String = "MediaMetadataRetriever"
        override val id: Int = RETRIEVER_ID_MEDIA_METADATA
        override fun retrieve(
            path: String,
            id: Long,
            context: Context,
            size: Size,
            raw: Boolean,
        ): FetchResult? {
            val retriever = AndroidMediaMetadataRetriever()
            return try {
                val bitmap = retrieveFromMediaMetadataRetriever(path, retriever, size, raw)
                bitmap?.let {
                    DrawableResult(
                        BitmapDrawable(context.resources, bitmap),
                        false,
                        DataSource.DISK
                    )
                }
            } finally {
                retriever.close()
            }
        }

        private fun retrieveFromMediaMetadataRetriever(
            filepath: String, retriever: AndroidMediaMetadataRetriever, size: Size, raw: Boolean,
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

    }


    class JAudioTaggerRetriever : ImageRetriever {
        override val name: String = "JAudioTaggerRetriever"
        override val id: Int = RETRIEVER_ID_J_AUDIO_TAGGER
        override fun retrieve(
            path: String, id: Long, context: Context, size: Size, raw: Boolean,
        ): FetchResult? {
            val bitmap = retrieveFromJAudioTagger(path, size, raw)
            return bitmap?.let {
                DrawableResult(
                    BitmapDrawable(context.resources, bitmap),
                    false,
                    DataSource.DISK
                )
            }
        }

        private fun retrieveFromJAudioTagger(filepath: String, size: Size, raw: Boolean): Bitmap? =
            try {
                val bytes = JAudioTaggerExtractor.readImage(File(filepath))
                if (bytes != null) {
                    if (raw) bytes.toBitmap() else bytes.toBitmap(size)
                } else {
                    null
                }
            } catch (e: Exception) {
                debug {
                    Log.i(name, "${e.javaClass.name}: ${e.message}")
                }
                null
            }
    }

    class ExternalFileRetriever : ImageRetriever {
        override val name: String = "ExternalFileRetriever"
        override val id: Int = RETRIEVER_ID_EXTERNAL_FILE
        override fun retrieve(
            path: String,
            id: Long, context: Context, size: Size, raw: Boolean,
        ): FetchResult? {
            return retrieveFromExternalFile(path)
        }

        private fun retrieveFromExternalFile(filepath: String): FetchResult? {
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

        private val folderCoverFiles = arrayOf(
            "cover.jpg",
            "album.jpg",
            "folder.jpg",
            "cover.png",
            "album.png",
            "folder.png"
        )
    }

}