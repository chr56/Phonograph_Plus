package player.phonograph.ui.views

import player.phonograph.R
import util.theme.internal.resolveColor
import androidx.appcompat.widget.AppCompatImageView
import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class IconImageView : AppCompatImageView {

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        setColorFilter(
            context.resolveColor(R.attr.iconColor, context.getColor(R.color.iconColor)),
            PorterDuff.Mode.SRC_IN
        )
    }
}
