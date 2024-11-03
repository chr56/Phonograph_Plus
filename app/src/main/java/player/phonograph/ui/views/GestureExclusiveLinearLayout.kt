/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.views

import androidx.appcompat.widget.LinearLayoutCompat
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.util.AttributeSet
import android.util.Log

class GestureExclusiveLinearLayout : LinearLayoutCompat {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) :
            super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)


    private val rect: Rect = Rect()
    private val exclusionAreaWidth: Int = 400

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (SDK_INT >= VERSION_CODES.Q) {
            rect.set(0, 0, exclusionAreaWidth, height)
            systemGestureExclusionRects = systemGestureExclusionRects + listOf(rect)
            player.phonograph.util.debug {
                Log.v("GestureExclusiveLayout", "Exclude: $rect")
            }
        }
    }
}