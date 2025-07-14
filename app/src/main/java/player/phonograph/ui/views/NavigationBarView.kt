package player.phonograph.ui.views

import androidx.core.view.WindowInsetsCompat
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets

class NavigationBarView : View {

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets, this)
        val navigationBarHeight = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        if (navigationBarHeight != layoutParams.height) {
            layoutParams = layoutParams.apply {
                height = navigationBarHeight
            }
        }
        return super.onApplyWindowInsets(insets)
    }

}
