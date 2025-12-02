/*
 *  Copyright (c) 2022~2025 chr_56, Aidan Follestad (afollestad) (original author)
 */

package player.phonograph.ui.views

import player.phonograph.util.theme.ThemeSettingsDelegate.accentColor
import player.phonograph.util.theme.ThemeSettingsDelegate.isNightTheme
import util.theme.view.checkbox.setTint
import androidx.appcompat.widget.AppCompatCheckBox
import android.content.Context
import android.util.AttributeSet

/**
 * @author Aidan Follestad (afollestad)
 */
class AccentColorCheckBox : AppCompatCheckBox {
    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, @Suppress("unused") attrs: AttributeSet?) {
        this.setTint(accentColor(), isNightTheme(context.resources))
    }
}