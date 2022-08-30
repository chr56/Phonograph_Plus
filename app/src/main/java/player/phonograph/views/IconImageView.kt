package player.phonograph.views

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import player.phonograph.R
import mt.util.color.resolveColor

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class IconImageView : AppCompatImageView {

    constructor(context: Context?) : super(context!!) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context?) {
        if (context == null) return
        setColorFilter(resolveColor(context, R.attr.iconColor), PorterDuff.Mode.SRC_IN)
    }
}
