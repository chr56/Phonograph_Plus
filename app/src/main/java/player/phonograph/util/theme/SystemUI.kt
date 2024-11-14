/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.App
import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import util.theme.activity.setTaskDescriptionColor
import util.theme.color.darkenColor
import util.theme.color.isColorLight
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import android.graphics.Color
import android.view.View


fun Activity.updateAllSystemUIColors(color: Int) {
    updateStatusbarColor(color)
    updateNavigationbarColor(color)
    updateTaskDescriptionColor(color)
}

/**
 * update Statusbar Color following current setting
 */
fun Activity.updateStatusbarColor(color: Int = darkenColor(primaryColor())) {
    val backgroundColor = darkenColor(color)
    @Suppress("DEPRECATION")
    window.statusBarColor = backgroundColor
    window.decorView.rootView.findViewById<View>(R.id.status_bar)?.setBackgroundColor(backgroundColor)
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isColorLight(backgroundColor)
}

/**
 * update Navigation Color following current setting
 */
fun Activity.updateNavigationbarColor(color: Int = darkenColor(primaryColor())) {
    val backgroundColor = if (coloredNavigationBar) color else Color.BLACK
    @Suppress("DEPRECATION")
    window.navigationBarColor = backgroundColor
    // window.decorView.rootView.findViewById<View>(R.id.navigation_bar)?.setBackgroundColor(backgroundColor)
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = isColorLight(backgroundColor)
}

fun Activity.updateTaskDescriptionColor(color: Int = darkenColor(primaryColor())) = setTaskDescriptionColor(color)


private var coloredNavigationBar: Boolean = Setting(App.instance)[Keys.coloredNavigationBar].data
fun updateColoredNavigationBarSettingCache(value: Boolean) {
    coloredNavigationBar = value
}
