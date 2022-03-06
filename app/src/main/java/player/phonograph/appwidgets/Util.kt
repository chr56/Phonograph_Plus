/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.appwidgets

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.RemoteViews
import player.phonograph.R
import player.phonograph.util.ImageUtil
import util.mddesign.util.MaterialColorHelper

object Util {

    fun createRoundedBitmap(drawable: Drawable?, width: Int, height: Int, tl: Float, tr: Float, bl: Float, br: Float): Bitmap? {
        if (drawable == null) return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(c)
        val rounded = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rounded)
        val paint = Paint()
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.isAntiAlias = true
        canvas.drawPath(composeRoundedRectPath(RectF(0F, 0F, width.toFloat(), height.toFloat()), tl, tr, bl, br), paint)
        return rounded
    }

    private fun composeRoundedRectPath(rect: RectF, tl: Float, tr: Float, bl: Float, br: Float): Path {
        val path = Path()
        val tl = if (tl < 0) 0F else tl
        val tr = if (tr < 0) 0F else tr
        val bl = if (bl < 0) 0F else bl
        val br = if (br < 0) 0F else br
        path.moveTo(rect.left + tl, rect.top)
        path.lineTo(rect.right - tr, rect.top)
        path.quadTo(rect.right, rect.top, rect.right, rect.top + tr)
        path.lineTo(rect.right, rect.bottom - br)
        path.quadTo(rect.right, rect.bottom, rect.right - br, rect.bottom)
        path.lineTo(rect.left + bl, rect.bottom)
        path.quadTo(rect.left, rect.bottom, rect.left, rect.bottom - bl)
        path.lineTo(rect.left, rect.top + tl)
        path.quadTo(rect.left, rect.top, rect.left + tl, rect.top)
        path.close()
        return path
    }

    fun RemoteViews.setDefaultPhonographWidgetAppearance(context: Context) {
        this.setViewVisibility(R.id.media_titles, View.INVISIBLE)
        this.setImageViewResource(R.id.image, R.drawable.default_album_art)
        this.setImageViewBitmap(
            R.id.button_next,
            ImageUtil.createBitmap(
                ImageUtil.getTintedVectorDrawable(
                    context, R.drawable.ic_skip_next_white_24dp, MaterialColorHelper.getPrimaryTextColor(context, false)
                )
            )
        )
        this.setImageViewBitmap(
            R.id.button_prev,
            ImageUtil.createBitmap(
                ImageUtil.getTintedVectorDrawable(
                    context, R.drawable.ic_skip_previous_white_24dp, MaterialColorHelper.getPrimaryTextColor(context, false)
                )
            )
        )
        this.setImageViewBitmap(
            R.id.button_toggle_play_pause,
            ImageUtil.createBitmap(
                ImageUtil.getTintedVectorDrawable(
                    context, R.drawable.ic_play_arrow_white_24dp, MaterialColorHelper.getPrimaryTextColor(context, false)
                )
            )
        )
    }
}
