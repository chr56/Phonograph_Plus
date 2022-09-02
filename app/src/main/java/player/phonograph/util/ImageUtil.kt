package player.phonograph.util

import android.content.Context
import android.content.res.Resources
import android.content.res.Resources.Theme
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import java.io.InputStream
import kotlin.math.roundToInt
import util.mddesign.util.TintHelper

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object ImageUtil {

    fun Bitmap.copy(): Bitmap? =
        try {
            this.copy(this.config ?: Bitmap.Config.RGB_565, false)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        }

    @Deprecated("rm")
    fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int): Int {
        // setting reqWidth matching to desired 1:1 ratio and screen-size
        val w: Int =
            if (width < height) {
                height / width * reqWidth
            } else {
                width / height * reqWidth
            }
        var inSampleSize = 1
        if (height > w || width > w) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize > w &&
                halfWidth / inSampleSize > w
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

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

    @JvmOverloads
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

    fun getVectorDrawable(res: Resources, @DrawableRes resId: Int, theme: Theme?): Drawable {
        return if (Build.VERSION.SDK_INT >= 21) {
            ResourcesCompat.getDrawable(res, resId, theme)!!
        } else VectorDrawableCompat.create(res, resId, theme)!!
    }

    fun getTintedVectorDrawable(
        res: Resources,
        @DrawableRes resId: Int,
        theme: Theme?,
        @ColorInt color: Int
    ): Drawable? =
        TintHelper.createTintedDrawable(getVectorDrawable(res, resId, theme), color)

    fun getTintedVectorDrawable(context: Context, @DrawableRes id: Int, @ColorInt color: Int): Drawable {
        return TintHelper.createTintedDrawable(
            getVectorDrawable(context.resources, id, context.theme),
            color
        )!!
    }

    fun getVectorDrawable(context: Context, @DrawableRes id: Int): Drawable =
        getVectorDrawable(context.resources, id, context.theme)

    fun resolveDrawable(context: Context, @AttrRes drawableAttr: Int): Drawable {
        val a = context.obtainStyledAttributes(intArrayOf(drawableAttr))
        val drawable = a.getDrawable(0)
        a.recycle()
        return drawable!!
    }

    fun resize(stream: InputStream?, scaledWidth: Int, scaledHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(
            BitmapFactory.decodeStream(stream),
            scaledWidth,
            scaledHeight,
            true
        )
    }

    fun Context.getTintedDrawable(
        @DrawableRes id: Int,
        @ColorInt color: Int,
        mode: BlendModeCompat = BlendModeCompat.SRC_IN
    ): Drawable? {
        val drawable = ResourcesCompat.getDrawable(this.resources, id, theme)
        drawable?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            color,
            mode
        )
        return drawable
    }
}
