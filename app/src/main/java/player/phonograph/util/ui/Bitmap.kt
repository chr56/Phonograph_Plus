/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.ui

import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import kotlin.math.roundToInt
import java.io.InputStream

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object BitmapUtil {

    fun Bitmap.restraintBitmapSize(maxSize: Int, filter: Boolean = false): Bitmap {
        val dstWidth: Int
        val dstHeight: Int
        if (width < height) {
            if (maxSize >= width) return this
            val ratio = height.toFloat() / width
            dstWidth = maxSize
            dstHeight = (maxSize * ratio).roundToInt()
        } else {
            if (maxSize >= height) return this
            val ratio = width.toFloat() / height
            dstWidth = (maxSize * ratio).roundToInt()
            dstHeight = maxSize
        }
        return scale(dstWidth, dstHeight, filter)
    }

    fun decodeBitmapFromStream(
        stream: InputStream?,
        scaledWidth: Int,
        scaledHeight: Int,
        filter: Boolean = false,
    ): Bitmap {
        return BitmapFactory.decodeStream(stream).scale(scaledWidth, scaledHeight, filter)
    }

    fun createBitmap(drawable: Drawable, sizeMultiplier: Float = 1f): Bitmap {
        val dstWidth = (drawable.intrinsicWidth * sizeMultiplier).toInt()
        val dstHeight = (drawable.intrinsicHeight * sizeMultiplier).toInt()
        return drawable.toBitmap(dstWidth, dstHeight)
    }
}
