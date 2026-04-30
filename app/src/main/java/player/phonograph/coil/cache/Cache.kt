/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.coil.cache

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
import player.phonograph.foundation.error.record
import player.phonograph.repo.room.domain.RoomImageCache
import player.phonograph.repo.room.entity.ImageCacheEntity
import player.phonograph.util.concurrent.lifecycleScopeOrNewOne
import player.phonograph.util.file.createOrOverrideFileRecursive
import androidx.core.graphics.drawable.toBitmapOrNull
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID


class CacheStore(val context: Context) {

    interface Cache<T : LoaderTarget> {
        fun set(target: T, data: FetchResult, source: Int)
        fun get(target: T, source: Int): FetchResult?
        fun markNoImage(target: T, source: Int)
        fun isNoImage(target: T, source: Int): Boolean
    }

    sealed class DefaultCache<T : LoaderTarget>(
        protected val context: Context,
        private val domain: Int,
    ) : Cache<T> {

        override fun isNoImage(target: T, source: Int): Boolean {
            val result = RoomImageCache.fetch(domain, target.id, source)
            return result.isEmpty()
        }

        override fun markNoImage(target: T, source: Int) {
            RoomImageCache.register(domain, target.id, source, null)
        }

        override fun set(
            target: T,
            data: FetchResult,
            source: Int,
        ) {
            val uuid = UUID.randomUUID().toString()

            val result = RoomImageCache.register(domain, target.id, source, uuid)

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
                record(context, e, TAG)
            }
        }

        override fun get(target: T, source: Int): FetchResult? {
            val uuid = RoomImageCache.fetch(domain, target.id, source).existedOrNull() ?: return null

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
                    record(context, e, TAG)
                    null
                }
            } else {
                null
            }

        }

    }


    class AudioFiles(context: Context) : DefaultCache<SongImage>(context, ImageCacheEntity.DOMAIN_SONG)
    class AlbumImages(context: Context) : DefaultCache<AlbumImage>(context, ImageCacheEntity.DOMAIN_ALBUM)
    class ArtistImages(context: Context) : DefaultCache<ArtistImage>(context, ImageCacheEntity.DOMAIN_ARTIST)


    companion object {

        fun clear(context: Context) {
            context.lifecycleScopeOrNewOne().launch(Dispatchers.IO) {
                RoomImageCache.clear()
                rootCacheDir(context).deleteRecursively()
            }
        }

        private const val TAG = "CacheStore"

        const val CACHE_DIR = "images"

        private fun rootCacheDir(context: Context) = context.cacheDir.resolve(CACHE_DIR)

    }
}
