/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose

import lib.phonograph.activity.MultiLanguageActivity
import lib.phonograph.theme.ThemeColor
import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.settings.ThemeSetting
import util.theme.activity.adjustStatusbarText
import util.theme.activity.setNavigationBarColor
import util.theme.activity.setStatusbarColor
import util.theme.color.darkenColor
import util.theme.materials.MaterialColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import util.theme.materials.R as MR

abstract class ComposeThemeActivity : MultiLanguageActivity() {

    protected val primaryColor: MutableStateFlow<Color> = MutableStateFlow(Color(MaterialColor.Blue._A400.asColor))
    protected val accentColor: MutableStateFlow<Color> = MutableStateFlow(Color(MaterialColor.Yellow._900.asColor))

    override fun onCreate(savedInstanceState: Bundle?) {

        primaryColor.value = Color(ThemeSetting.primaryColor(this))
        accentColor.value = Color(ThemeSetting.accentColor(this))

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            primaryColor.collect {
                onUpdatePrimaryColor(it.toArgb())
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            Setting(this@ComposeThemeActivity)[Keys.selectedPrimaryColor].flow.collect {
                primaryColor.value = Color(it)
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            Setting(this@ComposeThemeActivity)[Keys.selectedAccentColor].flow.collect {
                primaryColor.value = Color(it)
            }
        }

    }

    private fun onUpdatePrimaryColor(newPrimaryColor: Int) {
        val darkenPrimaryColor = darkenColor(newPrimaryColor)
        setStatusbarColor(darkenPrimaryColor, R.id.status_bar)
        adjustStatusbarText(darkenPrimaryColor)
        if (Setting(this)[Keys.coloredNavigationBar].data) setNavigationBarColor(newPrimaryColor)
    }
}