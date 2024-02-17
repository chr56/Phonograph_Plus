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
import player.phonograph.coil.model.AlbumImage
import player.phonograph.coil.model.ArtistImage
import player.phonograph.coil.model.LoaderTarget
import player.phonograph.coil.model.SongImage
import player.phonograph.util.file.createOrOverrideFileRecursive
import player.phonograph.util.recordThrowable
import androidx.core.graphics.drawable.toBitmapOrNull
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.util.UUID


class CacheStore(val context: Context) {

    interface Cache<T : LoaderTarget> {
        fun set(target: T, data: FetchResult, type: Int)
        fun get(target: T, type: Int): FetchResult?
        fun markNoImage(target: T, type: Int)
        fun isNoImage(target: T, type: Int): Boolean
    }

    sealed class DefaultCache<T : LoaderTarget>(
        protected val context: Context,
        private val tableName: CacheDatabase.Target,
    ) : Cache<T> {

        override fun isNoImage(target: T, type: Int): Boolean {
            val cacheDatabase = CacheDatabase.instance(context)
            val result = cacheDatabase.fetch(tableName, target.id, type)
            return result.isEmpty()
        }

        override fun markNoImage(target: T, type: Int) {
            val cacheDatabase = CacheDatabase.instance(context)
            cacheDatabase.register(tableName, target.id, type, null)
        }

        override fun set(
            target: T,
            data: FetchResult,
            type: Int,
        ) {
            val uuid = UUID.randomUUID().toString()

            val cacheDatabase = CacheDatabase.instance(context)

            val result = cacheDatabase.register(tableName, target.id, type, uuid)

            if (!result) {
                Log.i(TAG, "Failed to insert cache database")
                return
            }

            val targetFile = rootCacheDir(context).resolve(uuid).createOrOverrideFileRecursive()

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

        override fun get(target: T, type: Int): FetchResult? {

            val cacheDatabase = CacheDatabase.instance(context)

            val uuid = cacheDatabase.fetch(tableName, target.id, type).existedOrNull() ?: return null

            val targetFile = rootCacheDir(context).resolve(uuid)

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


    class AudioFiles(context: Context) : DefaultCache<SongImage>(context, CacheDatabase.Target.SONG)
    class AlbumImages(context: Context) : DefaultCache<AlbumImage>(context, CacheDatabase.Target.ALBUM)
    class ArtistImages(context: Context) : DefaultCache<ArtistImage>(context, CacheDatabase.Target.ARTIST)


    companion object {

        fun clear(context: Context) {
            CacheDatabase.instance(context).clear()
            rootCacheDir(context).deleteRecursively()
        }

        private const val TAG = "CacheStore"

        const val CACHE_DIR = "images"

        private fun rootCacheDir(context: Context) = context.cacheDir.resolve(CACHE_DIR)

    }
}