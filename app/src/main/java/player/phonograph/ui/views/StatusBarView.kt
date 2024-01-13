package player.phonograph.ui.views

import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets

class StatusBarView : View {

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        layoutParams =
            layoutParams.apply {
                height =
                    if (SDK_INT >= Build.VERSION_CODES.R) {
                        insets.getInsets(WindowInsets.Type.statusBars()).top
                    } else {
                        @Suppress("DEPRECATION")
                        insets.systemWindowInsetTop
                    }
            }
        return super.onApplyWindowInsets(insets)
    }
}
