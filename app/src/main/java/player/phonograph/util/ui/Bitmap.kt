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
import kotlinx.coroutines.yield
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


    const val MAX_BYTES_LIMITATION = 1_048_576L


    /**
     * Decodes a [Bitmap] from a [ByteArray] with size restrictions; If the image exceeds size limit,it will be
     * downsampled until it fits limitation.
     *
     * @param bytes The [ByteArray] to be decoded as [Bitmap].
     * @param maxBytes The maximum allowed size of the image in bytes.
     * @param maxPixels The maximum allowed number of pixels (width * height) for the image.
     * @return The decoded [Bitmap], or `null` if failed.
     */
    suspend fun decodeBitmapWithRestrictions(
        bytes: ByteArray,
        maxPixels: Int,
        maxBytes: Long = MAX_BYTES_LIMITATION,
    ): Bitmap? {
        if (bytes.isEmpty()) return null

        val sampleOptions = if (shouldDownSampling(bytes, maxBytes = maxBytes, maxPixels = maxPixels)) {
            var sample = 2
            while (measureBitmapSize(bytes, sample) > maxPixels && sample <= 32) {
                sample *= 2
                yield()
            }
            BitmapFactory.Options().apply { inSampleSize = sample }
        } else {
            null
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, sampleOptions)
    }

    private fun shouldDownSampling(bytes: ByteArray, maxBytes: Long, maxPixels: Int): Boolean {
        if (bytes.size > maxBytes) return true
        if (measureBitmapSize(bytes) > maxPixels) return true
        return false
    }

    /**
     * Measures the dimensions of an image from a byte array
     *
     * @param bytes The byte array containing the image data.
     * @param sample The sample size to use when decoding the image bounds.
     * @return The estimated size of the image in pixels (width * height)
     */
    fun measureBitmapSize(bytes: ByteArray, sample: Int = 1): Long {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inSampleSize = sample
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val srcWidth = options.outWidth
        val srcHeight = options.outHeight
        val size = srcHeight.toLong() * srcWidth
        return size
    }

    /**
     * Measures the dimensions of an image from a byte array
     *
     * @param bytes The byte array containing the image data.
     * @param sample The sample size to use when decoding the image bounds.
     * @return The estimated dimensions of the image (width * height)
     */
    fun measureBitmapDimensions(bytes: ByteArray, sample: Int = 1): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inSampleSize = sample
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return options.outWidth to options.outWidth
    }

}
