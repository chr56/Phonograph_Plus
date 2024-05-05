/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose

import lib.phonograph.activity.MultiLanguageActivity
import player.phonograph.settings.ThemeSetting
import player.phonograph.util.theme.updateNavigationbarColor
import player.phonograph.util.theme.updateStatusbarColor
import player.phonograph.util.theme.updateTaskDescriptionColor
import util.theme.color.darkenColor
import util.theme.materials.MaterialColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

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

        lifecycleScope.launch {
            ThemeSetting.observeColors(this@ComposeThemeActivity) { primary, accent ->
                primaryColor.value = Color(primary)
                accentColor.value = Color(accent)
            }
        }

    }

    private fun onUpdatePrimaryColor(newPrimaryColor: Int) {
        val darkenPrimaryColor = darkenColor(newPrimaryColor)
        updateStatusbarColor(darkenPrimaryColor)
        updateNavigationbarColor(darkenPrimaryColor)
        updateTaskDescriptionColor(darkenPrimaryColor)
    }
}