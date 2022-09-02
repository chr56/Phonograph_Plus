/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RSRuntimeException
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import androidx.annotation.FloatRange
import coil.size.Size
import coil.transform.Transformation
import player.phonograph.BuildConfig
import util.phonograph.misc.StackBlur


/**
 * @param sampling The inSampleSize to use. Must be a power of 2, or 1 for no down sampling or 0 for auto detect sampling. Default is 0.
 * @param blurRadius The radius to use. Must be between 0 and 25. Default is 5.
 * @author Karim Abou Zeid (kabouzeid)
 */
@Suppress("DEPRECATION")
class BlurTransformation(
    private val context: Context,
    private val sampling: Int = 0,
    @FloatRange(from = 0.0, to = 25.0)
    private val blurRadius: Float = 5f,
) : Transformation {

    override val cacheKey get() = "Blur:sampling$sampling:blurRadius$blurRadius"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap =
        transform(input) ?: input

    private fun transform(toTransform: Bitmap): Bitmap? {
        val sampling: Int =
            if (this.sampling == 0) {
                calculateInSampleSize(toTransform.width, toTransform.height, 100)
            } else {
                this.sampling
            }

        val width = toTransform.width
        val height = toTransform.height

        val scaledWidth = width / sampling
        val scaledHeight = height / sampling

        val out: Bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(out)
        canvas.scale(1 / sampling.toFloat(), 1 / sampling.toFloat())
        val paint = Paint()
        paint.flags = Paint.FILTER_BITMAP_FLAG
        canvas.drawBitmap(toTransform, 0f, 0f, paint)

        try {
            val rs = RenderScript.create(context.applicationContext)
            val input =
                Allocation.createFromBitmap(rs, out, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT)
            val output = Allocation.createTyped(rs, input.type)
            val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            script.setRadius(blurRadius)
            script.setInput(input)
            script.forEach(output)
            output.copyTo(out)
            rs.destroy()
            return out
        } catch (e: Exception) {
            if (e is RSRuntimeException) Log.e("RenderScript", "RenderScript Error")
            if (BuildConfig.DEBUG) Log.v("Blur", e.message.orEmpty())
            return StackBlur.blur(out, blurRadius)
        }
    }

    companion object {
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

    }
}
