package player.phonograph.util

import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import kotlin.math.roundToInt
import java.io.InputStream

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object ImageUtil {

    fun resizeBitmap(src: Bitmap, maxForSmallerSize: Int): Bitmap {
        val width = src.width
        val height = src.height
        val dstWidth: Int
        val dstHeight: Int
        if (width < height) {
            if (maxForSmallerSize >= width) {
                return src
            }
            val ratio = height.toFloat() / width
            dstWidth = maxForSmallerSize
            dstHeight = (maxForSmallerSize * ratio).roundToInt()
        } else {
            if (maxForSmallerSize >= height) {
                return src
            }
            val ratio = width.toFloat() / height
            dstWidth = (maxForSmallerSize * ratio).roundToInt()
            dstHeight = maxForSmallerSize
        }
        return Bitmap.createScaledBitmap(src, dstWidth, dstHeight, false)
    }

    fun createBitmap(drawable: Drawable, sizeMultiplier: Float = 1f): Bitmap {
        val bitmap = Bitmap.createBitmap(
            (drawable.intrinsicWidth * sizeMultiplier).toInt(),
            (drawable.intrinsicHeight * sizeMultiplier).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val c = Canvas(bitmap)
        drawable.setBounds(0, 0, c.width, c.height)
        drawable.draw(c)
        return bitmap
    }

    fun resizeBitmap(stream: InputStream?, scaledWidth: Int, scaledHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(
            BitmapFactory.decodeStream(stream),
            scaledWidth,
            scaledHeight,
            true
        )
    }

}
