/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */
package lib.phonograph.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.min
import player.phonograph.R

class BorderCircleView : FrameLayout {

    private val paint: Paint = Paint().apply { isAntiAlias = true }
    private val paintBorder: Paint = Paint().apply { isAntiAlias = true; color = Color.BLACK }

    private val mCheck: Drawable = ContextCompat.getDrawable(context, R.drawable.ic_check)!!
    private val borderWidth: Int = resources.getDimension(R.dimen.circleview_border).toInt()

    private var paintCheck: Paint? = null

    init {
        setWillNotDraw(false)
    }

    constructor(context: Context) : this(context, null, 0) { }
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) { }
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        super(context, attrs, defStyleAttr) { }

    override fun setBackgroundColor(color: Int) {
        paint.color = color
        requestLayout()
        invalidate()
    }

    var borderColor: Int
        get() = paintBorder.color
        set(color) {
            paintBorder.color = color
            requestLayout()
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
            setMeasuredDimension(
                MeasureSpec.getSize(widthMeasureSpec),
                if (heightMode == MeasureSpec.AT_MOST) {
                    min(
                        MeasureSpec.getSize(widthMeasureSpec),
                        MeasureSpec.getSize(heightMeasureSpec)
                    )
                } else {
                    MeasureSpec.getSize(widthMeasureSpec)
                }
            )
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    @SuppressLint("CanvasSize")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val canvasSize = min(canvas.height, canvas.width)

        val circleCenter = (canvasSize - borderWidth * 2) / 2

        // outer
        canvas.drawCircle(
            (circleCenter + borderWidth).toFloat(),
            (circleCenter + borderWidth).toFloat(),
            (canvasSize - borderWidth * 2) / 2 + borderWidth - 4.0f,
            paintBorder
        )
        // inner
        canvas.drawCircle(
            (circleCenter + borderWidth).toFloat(),
            (circleCenter + borderWidth).toFloat(),
            (canvasSize - borderWidth * 2) / 2 - 4.0f,
            paint
        )

        if (isActivated) {
            val offset = canvasSize / 2 - mCheck.intrinsicWidth / 2

            if (paintCheck == null) {
                paintCheck = Paint().apply { isAntiAlias = true }
            }

            paintCheck!!.colorFilter = if (paint.color == Color.WHITE) blackFilter else whiteFilter
            mCheck.setBounds(
                offset,
                offset,
                mCheck.intrinsicWidth - offset,
                mCheck.intrinsicHeight - offset
            )
            mCheck.draw(canvas)
        }
    }

    private val blackFilter: PorterDuffColorFilter by lazy(NONE) {
        PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
    }
    private val whiteFilter: PorterDuffColorFilter by lazy(NONE) {
        PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
    }
}
