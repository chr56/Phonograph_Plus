/*
 * Copyright (c) 2022~2023 chr_56
 */
@file:JvmName("ImageRetriever")

package player.phonograph.coil.retriever

import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.size.Size
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
    ): FetchResult?
}

class MediaStoreRetriever : ImageRetriever {
    override val name: String = "MediaStoreRetriever"
    override fun retrieve(
        path: String,
        id: Long,
        context: Context,
        size: Size
    ): FetchResult? {
        return readFromMediaStore(id, context, size)
    }
}

class MediaMetadataRetriever : ImageRetriever {
    override val name: String = "MediaMetadataRetriever"
    override fun retrieve(
        path: String,
        id: Long,
        context: Context,
        size: Size
    ): FetchResult? {
        val bitmap = MediaMetadataRetriever().use {
            retrieveFromMediaMetadataRetriever(path, it, size)
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
    override fun retrieve(
        path: String,
        id: Long, context: Context, size: Size
    ): FetchResult? {
        val bitmap = retrieveFromJAudioTagger(path, size)
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
    override fun retrieve(
        path: String,
        id: Long, context: Context, size: Size
    ): FetchResult? {
        return retrieveFromExternalFile(path)
    }
}