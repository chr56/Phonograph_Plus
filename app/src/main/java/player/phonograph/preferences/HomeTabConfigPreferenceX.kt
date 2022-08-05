package player.phonograph.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.Keep
import androidx.preference.DialogPreference
import player.phonograph.R

@Keep
class HomeTabConfigPreferenceX : DialogPreference {
    @Keep
    constructor(context: Context?) : super(context!!) {
        init()
    }

    @Keep
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    @Keep
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr) {
        init()
    }

    @Keep
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context!!, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        layoutResource = R.layout.x_preference
    }
}