/*
 * Copyright (c) 2022~2023 chr_56
 */
@file:JvmName("ImageRetriever")
package player.phonograph.coil.retriever

import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.size.Size
import player.phonograph.coil.audiofile.AudioFile
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever

interface ImageRetriever {
    val name: String
    fun retrieve(
        audioFile: AudioFile,
        context: Context,
        size: Size,
    ): FetchResult?
}

class MediaStoreRetriever : ImageRetriever {
    override val name: String = "MediaStoreRetriever"
    override fun retrieve(audioFile: AudioFile, context: Context, size: Size): FetchResult? {
        return readFromMediaStore(audioFile.albumId, context, size)
    }
}

class MediaMetadataRetriever : ImageRetriever {
    override val name: String = "MediaMetadataRetriever"
    override fun retrieve(audioFile: AudioFile, context: Context, size: Size): FetchResult? {
        val bitmap = retrieveFromMediaMetadataRetriever(
            audioFile.path, mediaMetadataRetriever, size
        )
        return bitmap?.let {
            DrawableResult(
                BitmapDrawable(context.resources, bitmap),
                false,
                DataSource.DISK
            )
        }
    }
    companion object {
        private val mediaMetadataRetriever = MediaMetadataRetriever()
    }
}


class JAudioTaggerRetriever : ImageRetriever {
    override val name: String = "JAudioTaggerRetriever"
    override fun retrieve(audioFile: AudioFile, context: Context, size: Size): FetchResult? {
        val bitmap = retrieveFromJAudioTagger(
            audioFile.path, size
        )
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
    override fun retrieve(audioFile: AudioFile, context: Context, size: Size): FetchResult? {
        return retrieveFromExternalFile(audioFile.path)
    }
}