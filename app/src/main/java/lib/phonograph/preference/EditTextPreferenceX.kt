/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */
package lib.phonograph.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import player.phonograph.R

/**
 * @author Aidan Follestad (afollestad)
 */
class EditTextPreferenceX : EditTextPreference {
    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context,
        attrs,
        defStyleAttr,
        defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        layoutResource = R.layout.x_preference
    }
}