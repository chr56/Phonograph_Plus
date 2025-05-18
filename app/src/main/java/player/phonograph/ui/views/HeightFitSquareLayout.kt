package player.phonograph.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class HeightFitSquareLayout : FrameLayout {
    private var forceSquare = true

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(if (forceSquare) heightMeasureSpec else widthMeasureSpec, heightMeasureSpec)
    }

    fun forceSquare(forceSquare: Boolean) {
        this.forceSquare = forceSquare
        requestLayout()
    }
}