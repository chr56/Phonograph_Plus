package player.phonograph.ui.views

import androidx.core.view.WindowInsetsCompat
import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.AttributeSet
import android.view.WindowInsets
import android.widget.FrameLayout

class StatusBarMarginFrameLayout : FrameLayout {

    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr) {}

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets, this)
        layoutParams = (layoutParams as MarginLayoutParams).apply {
            topMargin = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.statusBars()).top
        }
        return super.onApplyWindowInsets(insets)
    }
}
