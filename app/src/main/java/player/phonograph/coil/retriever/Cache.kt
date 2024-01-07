/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.retriever

import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.SourceResult
import okio.buffer
import okio.sink
import okio.source
import player.phonograph.coil.album.AlbumImage
import player.phonograph.coil.artist.ArtistImage
import player.phonograph.coil.audiofile.AudioFile
import player.phonograph.util.file.createOrOverrideFileRecursive
import player.phonograph.util.recordThrowable
import androidx.core.graphics.drawable.toBitmapOrNull
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File


class CacheStore(val context: Context) {

    interface Cache<T> {
        val directory: File
        fun item(target: T): File
        fun set(target: T, data: FetchResult, type: String)
        fun get(target: T, type: String): FetchResult?
        fun markNoImage(target: T, type: Int)
        fun isNoImage(target: T, type: Int): Boolean
    }

    sealed class DefaultCache<T>(protected val context: Context) : Cache<T> {

        protected abstract fun table(): CacheDatabase.Target
        protected abstract fun id(target: T): Long

        override fun isNoImage(target: T, type: Int): Boolean {
            val cacheDatabase = CacheDatabase.instance(context)
            val result = cacheDatabase.isNoImage(table(), id(target), type)
            // cacheDatabase.release()
            return result
        }

        override fun markNoImage(target: T, type: Int) {
            val cacheDatabase = CacheDatabase.instance(context)
            cacheDatabase.markNoImage(table(), id(target), type)
            // cacheDatabase.release()
        }

        override fun set(
            target: T,
            data: FetchResult,
            type: String,
        ) {
            val targetFile = item(target).resolve(type).createOrOverrideFileRecursive()

            try {
                targetFile.sink().buffer().use { sink ->
                    when (data) {
                        is SourceResult   -> {
                            val bufferedSource = data.source.source()
                            bufferedSource.readAll(sink)
                        }

                        is DrawableResult -> {
                            val bitmap = data.drawable.toBitmapOrNull()
                            if (bitmap != null && bitmap.height > 0 && bitmap.width > 0) {
                                val outputStream = sink.outputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                            } else {
                                Log.v(TAG, "Drawable of $data is not available!")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                recordThrowable(context, TAG, e)
            }
        }

        override fun get(target: T, type: String): FetchResult? {
            val targetFile = item(target).resolve(type)

            return if (targetFile.exists() && targetFile.isFile) {
                try {
                    val bufferedSource = targetFile.source().buffer()
                    SourceResult(
                        source = ImageSource(
                            source = bufferedSource,
                            context = context
                        ),
                        mimeType = "image/jpeg",
                        dataSource = DataSource.DISK
                    )
                } catch (e: Exception) {
                    recordThrowable(context, TAG, e)
                    null
                }
            } else {
                null
            }

        }

    }


    class AudioFiles(context: Context) : DefaultCache<AudioFile>(context) {

        override val directory: File = rootCacheDir(context).resolve(AUDIO_FILES_CACHE_DIR)
        override fun item(target: AudioFile): File = directory.resolve(target.songId.toString())

        override fun table(): CacheDatabase.Target = CacheDatabase.Target.SONG
        override fun id(target: AudioFile): Long = target.songId
    }

    class AlbumImages(context: Context) : DefaultCache<AlbumImage>(context) {

        override val directory: File = rootCacheDir(context).resolve(ALBUMS_CACHE_DIR)
        override fun item(target: AlbumImage): File = directory.resolve(target.id.toString())

        override fun table(): CacheDatabase.Target = CacheDatabase.Target.ALBUM
        override fun id(target: AlbumImage): Long = target.id
    }

    class ArtistImages(context: Context) : DefaultCache<ArtistImage>(context) {

        override val directory: File = rootCacheDir(context).resolve(ARTISTS_CACHE_DIR)
        override fun item(target: ArtistImage): File = directory.resolve(target.id.toString())

        override fun table(): CacheDatabase.Target = CacheDatabase.Target.ARTIST
        override fun id(target: ArtistImage): Long = target.id
    }

    fun clear(context: Context) {
        rootCacheDir(context).deleteRecursively()
        CacheDatabase.instance(context).clear()
    }

    companion object {
        private const val TAG = "CacheStore"

        const val CACHE_DIR = "images"

        private fun rootCacheDir(context: Context) = context.cacheDir.resolve(CACHE_DIR)

        private const val AUDIO_FILES_CACHE_DIR = "audio_files"
        private const val ALBUMS_CACHE_DIR = "albums"
        private const val ARTISTS_CACHE_DIR = "artists"

        private const val NO_IMAGES = "EMPTY"
    }
}