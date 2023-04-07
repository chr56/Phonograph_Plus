/*
 * Copyright (c) 2022 Abou Zeid (kabouzeid) (original author)
 */
package lib.phonograph.view

import mt.pref.ThemeColor.accentColor
import mt.tint.viewtint.setTint
import player.phonograph.util.theme.nightMode
import androidx.appcompat.widget.AppCompatCheckBox
import android.content.Context
import android.util.AttributeSet

/**
 * @author Aidan Follestad (afollestad)
 */
class CheckBoxX : AppCompatCheckBox {
    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        this.setTint(accentColor(context), context.nightMode)
    }
}