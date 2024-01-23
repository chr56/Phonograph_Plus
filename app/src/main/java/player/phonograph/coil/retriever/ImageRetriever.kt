/*
 * Copyright (c) 2022~2023 chr_56
 */
@file:JvmName("ImageRetrievers")

package player.phonograph.coil.retriever

import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.size.Size
import androidx.annotation.IntDef
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever

interface ImageRetriever {
    val name: String
    fun retrieve(
        path: String,
        id: Long,
        context: Context,
        size: Size,
        raw: Boolean,
    ): FetchResult?

    val id: Int
}

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
        return retrieveFromMediaStore(id, context, size)
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
        val bitmap = MediaMetadataRetriever().use {
            retrieveFromMediaMetadataRetriever(path, it, size, raw)
        }
        return bitmap?.let {
            DrawableResult(
                BitmapDrawable(context.resources, bitmap),
                false,
                DataSource.DISK
            )
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
}

const val RETRIEVER_ID_MEDIA_STORE = 0
const val RETRIEVER_ID_MEDIA_METADATA = 1
const val RETRIEVER_ID_J_AUDIO_TAGGER = 4
const val RETRIEVER_ID_EXTERNAL_FILE = 7

fun allRetrieverId() = intArrayOf(
    RETRIEVER_ID_MEDIA_STORE,
    RETRIEVER_ID_MEDIA_METADATA,
    RETRIEVER_ID_J_AUDIO_TAGGER,
    RETRIEVER_ID_EXTERNAL_FILE,
)


@IntDef(RETRIEVER_ID_MEDIA_STORE, RETRIEVER_ID_MEDIA_METADATA, RETRIEVER_ID_J_AUDIO_TAGGER, RETRIEVER_ID_EXTERNAL_FILE)
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class RetrieverId