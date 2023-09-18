/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose

import lib.phonograph.activity.MultiLanguageActivity
import mt.pref.ThemeColor
import mt.tint.requireLightStatusbarAuto
import mt.tint.setNavigationBarColor
import mt.tint.setStatusbarColor
import mt.util.color.darkenColor
import player.phonograph.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class ComposeThemeActivity : MultiLanguageActivity() {

    protected val primaryColor: MutableStateFlow<Color> = MutableStateFlow(Color(mt.color.R.color.md_cyan_A700))
    protected val accentColor: MutableStateFlow<Color> = MutableStateFlow(Color(mt.color.R.color.md_yellow_400))

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
        requireLightStatusbarAuto(darkenPrimaryColor)
        setNavigationBarColor(newPrimaryColor)
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