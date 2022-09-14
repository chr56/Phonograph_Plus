package player.phonograph.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.ColorInt
import mt.util.color.darkenColor
import mt.util.color.isColorLight
import mt.util.color.lightenColor
import player.phonograph.R
import player.phonograph.util.preferences.StyleConfig


/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object PhonographColorUtil {

    @JvmStatic
    val Resources.nightMode: Boolean
        get() = when (StyleConfig.generalTheme) {
            R.style.Theme_Phonograph_Black -> true
            R.style.Theme_Phonograph_Dark -> true
            R.style.Theme_Phonograph_Light -> false
            R.style.Theme_Phonograph_Auto -> when (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                else -> false
            }
            else -> false
        }

    fun Context.backgroundColorByTheme(): Int = resources.getColor(
        when (StyleConfig.generalTheme) {
            R.style.Theme_Phonograph_Auto -> R.color.cardBackgroundColor
            R.style.Theme_Phonograph_Light -> R.color.md_white_1000
            R.style.Theme_Phonograph_Black -> R.color.md_black_1000
            R.style.Theme_Phonograph_Dark -> R.color.md_grey_800
            else -> R.color.md_grey_700
        },
        theme
    )

    @JvmStatic
    @ColorInt
    fun shiftBackgroundColorForLightText(@ColorInt backgroundColor: Int): Int {
        var newColor = backgroundColor
        while (isColorLight(newColor)) newColor = darkenColor(newColor)
        return newColor
    }

    @ColorInt
    fun shiftBackgroundColorForDarkText(@ColorInt backgroundColor: Int): Int {
        var newColor = backgroundColor
        while (!isColorLight(newColor)) newColor = lightenColor(newColor)
        return newColor
    }
}
