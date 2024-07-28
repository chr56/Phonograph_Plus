/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.service.notification

import coil.Coil
import coil.request.Disposable
import coil.request.ImageRequest
import coil.size.Size
import player.phonograph.R
import player.phonograph.coil.target.PaletteBitmap
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.Song
import androidx.core.graphics.drawable.toBitmapOrNull
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Build.VERSION_CODES
import android.util.LruCache

/**
 * Component handling image load
 */
class CoverLoader(private val context: Context) {

    private val cache = LruCache<Long, PaletteBitmap>(4)
    private val loader = Coil.imageLoader(context)

    fun load(song: Song, callback: (Bitmap?, Int) -> Unit): Disposable? {
        val cachedImage: PaletteBitmap? = cache[song.id]
        if (cachedImage != null) {
            // cache hit
            callback(cachedImage.bitmap, cachedImage.paletteColor)
            return null
        } else {
            // cache missed
            val imageRequest =
                ImageRequest.Builder(context)
                    .data(song)
                    .properSize()
                    .target(
                        PaletteTargetBuilder(context)
                            .onResourceReady { result, paletteColor ->
                                val bitmap =
                                    if (result is BitmapDrawable) result.bitmap else result.toBitmapOrNull()
                                if (bitmap != null) {
                                    cache.put(song.id, PaletteBitmap(bitmap, paletteColor))
                                }
                                callback(bitmap, paletteColor)
                            }
                            .build()
                    )
                    .build()
            return loader.enqueue(imageRequest)
        }
    }

    fun getCache(songId: Long) = cache[songId].bitmap

    /**
     * size correct size
     */
    private fun ImageRequest.Builder.properSize(): ImageRequest.Builder {
        if (Build.VERSION.SDK_INT < VERSION_CODES.P) size(largeIconSize)
        return this
    }

    internal val defaultCover: Bitmap by lazy(LazyThreadSafetyMode.NONE) {
        BitmapFactory.decodeResource(context.resources, R.drawable.default_album_art)
    }

    private val largeIconSize: Size by lazy(LazyThreadSafetyMode.NONE) {
        with(context.resources) {
            Size(
                getDimensionPixelSize(androidx.core.R.dimen.notification_large_icon_width),
                getDimensionPixelSize(androidx.core.R.dimen.notification_large_icon_height)
            )
        }
    }

    fun terminate() {
        cache.evictAll()
    }
}