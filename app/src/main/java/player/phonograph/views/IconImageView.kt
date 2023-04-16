package player.phonograph.views

import mt.pref.ThemeColor
import mt.util.color.resolveColor
import player.phonograph.R
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
            resolveColor(context, R.attr.iconColor, ThemeColor.accentColor(context)),
            PorterDuff.Mode.SRC_IN
        )
    }
}
