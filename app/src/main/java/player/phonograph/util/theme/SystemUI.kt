/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.App
import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import util.theme.activity.setNavigationBarColor
import util.theme.activity.setStatusbarColor
import util.theme.activity.setTaskDescriptionColor
import util.theme.color.darkenColor
import android.app.Activity
import android.graphics.Color
import android.view.View

/**
 * update Statusbar Color following current setting
 */
fun Activity.updateStatusbarColor(color: Int = darkenColor(primaryColor())) {
    setStatusbarColor(darkenColor(color), R.id.status_bar)
}

/**
 * update Navigation Color following current setting
 */
fun Activity.updateNavigationbarColor(color: Int = darkenColor(primaryColor())) {
    val targetColor = if (coloredNavigationBar) color else Color.BLACK
    setNavigationBarColor(targetColor)
}

fun Activity.updateTaskDescriptionColor(color: Int = darkenColor(primaryColor())) = setTaskDescriptionColor(color)


private var coloredNavigationBar: Boolean = Setting(App.instance)[Keys.coloredNavigationBar].data
fun updateColoredNavigationBarSettingCache(value: Boolean) {
    coloredNavigationBar = value
}

@Suppress("DEPRECATION")
fun Activity.setFullScreenAndIncludeStatusBar() {
    window.decorView.systemUiVisibility =
        (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
}

@Suppress("DEPRECATION")
fun Activity.restoreNotFullsScreen() {
    window.decorView.systemUiVisibility -=
        (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
}