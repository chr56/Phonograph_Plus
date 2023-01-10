/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil

import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.size.Dimension
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
            audioFile.path,
            MediaMetadataRetriever(),
            size.w(), size.h()
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


class JAudioTaggerRetriever : ImageRetriever {
    override val name: String = "JAudioTaggerRetriever"
    override fun retrieve(audioFile: AudioFile, context: Context, size: Size): FetchResult? {
        val bitmap = retrieveFromJAudioTagger(
            audioFile.path, size.w(), size.h()
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
        val bitmap = retrieveFromExternalFile(
            audioFile.path, size.w(), size.h()
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

//todo

private fun Size.w() = (width as? Dimension.Pixels)?.px ?: -1
private fun Size.h() = (height as? Dimension.Pixels)?.px ?: -1