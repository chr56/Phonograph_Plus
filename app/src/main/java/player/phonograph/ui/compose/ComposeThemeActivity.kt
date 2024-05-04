/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose

import lib.phonograph.activity.MultiLanguageActivity
import lib.phonograph.theme.ThemeColor
import player.phonograph.R
import util.theme.activity.adjustStatusbarText
import util.theme.activity.setNavigationBarColor
import util.theme.activity.setStatusbarColor
import util.theme.color.darkenColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class ComposeThemeActivity : MultiLanguageActivity() {

    protected val primaryColor: MutableStateFlow<Color> = MutableStateFlow(Color(util.theme.materials.R.color.md_cyan_A700))
    protected val accentColor: MutableStateFlow<Color> = MutableStateFlow(Color(util.theme.materials.R.color.md_yellow_400))

    override fun onCreate(savedInstanceState: Bundle?) {

        primaryColor.value = Color(ThemeColor.primaryColor(this))
        accentColor.value = Color(ThemeColor.accentColor(this))
        ThemeColor.registerPreferenceChangeListener(listener, this.applicationContext, this)

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            primaryColor.collect {
                onUpdatePrimaryColor(it.toArgb())
            }
        }

    }

    private fun onUpdatePrimaryColor(newPrimaryColor: Int) {
        val darkenPrimaryColor = darkenColor(newPrimaryColor)
        setStatusbarColor(darkenPrimaryColor, R.id.status_bar)
        adjustStatusbarText(darkenPrimaryColor)
        if (ThemeColor.coloredNavigationBar(this)) setNavigationBarColor(newPrimaryColor)
    }

    private val listener = object : ThemeColor.ThemePreferenceChangeListener {

        override fun onAccentColorChanged(newColor: Int) {
            accentColor.value = Color(newColor)
        }

        override fun onPrimaryColorChanged(newColor: Int) {
            primaryColor.value = Color(newColor)
        }

        override fun onNavigationBarTintSettingChanged(coloredNavigationBar: Boolean) {
        }

        override fun onStatusBarTintSettingChanged(coloredStatusBar: Boolean) {
        }

    }


}