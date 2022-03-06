package player.phonograph.misc

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks
import com.github.ksoichiro.android.observablescrollview.ScrollState

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class SimpleObservableScrollViewCallbacks : ObservableScrollViewCallbacks {
    override fun onScrollChanged(i: Int, b: Boolean, b2: Boolean) {}
    override fun onDownMotionEvent() {}
    override fun onUpOrCancelMotionEvent(scrollState: ScrollState) {}
}
