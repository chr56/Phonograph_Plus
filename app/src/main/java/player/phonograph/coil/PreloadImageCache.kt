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

    private val imageCache: LruCache<Long, PaletteBitmap> = LruCache(size) // SongId <-> PaletteBitmap

    private fun putCache(songId: Long, bitmap: Bitmap, color: Int) {
        imageCache.put(songId, PaletteBitmap(bitmap, color))
    }

    private suspend fun loadAndStore(context: Context, song: Song, timeout: Long): PaletteBitmap {
        val loaded = loadImage(context, song, timeout)
        putCache(song.id, loaded.bitmap, loaded.paletteColor)
        return loaded
    }

    suspend fun fetchPaletteColor(context: Context, song: Song): Int {
        val cached = imageCache[song.id]?.paletteColor
        return if (cached == null) {
            val loaded: PaletteBitmap = loadAndStore(context, song, timeout = 2000)
            loaded.paletteColor
        } else {
            cached
        }
    }

    suspend fun fetchBitmap(context: Context, song: Song): Bitmap {
        val cached = imageCache[song.id]?.bitmap
        return if (cached == null) {
            val loaded: PaletteBitmap = loadAndStore(context, song, timeout = 2000)
            loaded.bitmap
        } else {
            cached
        }
    }

    suspend fun preload(context: Context, song: Song, timeout: Long) {
        loadAndStore(context, song, timeout)
    }
}