package player.phonograph.ui.views

import androidx.core.view.WindowInsetsCompat
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets

class NavigationBarView : View {

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets, this)
        layoutParams = layoutParams.apply {
            height = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        }
        return super.onApplyWindowInsets(insets)
    }

}
