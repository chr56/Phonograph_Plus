/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.coil

import player.phonograph.coil.target.PaletteBitmap
import player.phonograph.model.Song
import androidx.collection.LruCache
import android.content.Context
import android.graphics.Bitmap

class PreloadImageCache(size: Int) {

    private val imageCache: LruCache<Song, PaletteBitmap> = LruCache(size)

    private fun putCache(song: Song, bitmap: Bitmap, color: Int) {
        imageCache.put(song, PaletteBitmap(bitmap, color))
    }

    private fun getPaletteColorFromCache(song: Song) = imageCache[song]?.paletteColor
    private fun getImageFromCache(song: Song) = imageCache[song]?.bitmap

    suspend fun fetchPaletteColor(context: Context, song: Song): Int {
        val cached = getPaletteColorFromCache(song)
        return if (cached == null) {
            val loaded = loadImage(context, song, timeout = 2000)
            putCache(song, loaded.bitmap, loaded.paletteColor)
            loaded.paletteColor
        } else {
            cached
        }
    }

    suspend fun fetchBitmap(context: Context, song: Song): Bitmap {
        val cached = getImageFromCache(song)
        return if (cached == null) {
            val loaded = loadImage(context, song, timeout = 2000)
            putCache(song, loaded.bitmap, loaded.paletteColor)
            loaded.bitmap
        } else {
            cached
        }
    }
}