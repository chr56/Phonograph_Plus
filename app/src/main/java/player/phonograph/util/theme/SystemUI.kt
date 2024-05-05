/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.App
import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.settings.ThemeSetting.primaryColor
import util.theme.activity.setNavigationBarColor
import util.theme.activity.setStatusbarColor
import util.theme.activity.setTaskDescriptionColor
import util.theme.color.darkenColor
import android.app.Activity
import android.graphics.Color


fun Activity.updateStatusbarColor() = updateStatusbarColor(darkenColor(primaryColor()))

fun Activity.updateStatusbarColor(color: Int) {
    setStatusbarColor(darkenColor(color), R.id.status_bar)
}

fun Activity.updateNavigationbarColor() = updateNavigationbarColor(darkenColor(primaryColor()))

fun Activity.updateNavigationbarColor(color: Int) {
    val targetColor = if (coloredNavigationBar) color else Color.BLACK
    setNavigationBarColor(targetColor)
}


fun Activity.updateTaskDescriptionColor() = updateTaskDescriptionColor(darkenColor(primaryColor()))
fun Activity.updateTaskDescriptionColor(color: Int) = setTaskDescriptionColor(color)


private val coloredNavigationBar: Boolean get() = Setting(App.instance)[Keys.coloredNavigationBar].data