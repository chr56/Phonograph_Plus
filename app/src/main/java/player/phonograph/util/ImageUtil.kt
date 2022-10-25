package player.phonograph.util

import mt.util.color.primaryTextColor
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
import mt.util.drawable.createTintedDrawable
import java.io.InputStream
import kotlin.math.roundToInt

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
        createTintedDrawable(getVectorDrawable(res, resId, theme), color)

    fun getTintedVectorDrawable(context: Context, @DrawableRes id: Int, @ColorInt color: Int): Drawable {
        return createTintedDrawable(
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

    fun Context.makeContrastDrawable(
        source: Drawable?,
        backgroundColor: Int,
    ): Drawable? {
        source?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            primaryTextColor(backgroundColor),
            BlendModeCompat.SRC_IN
        )
        return source
    }
}
