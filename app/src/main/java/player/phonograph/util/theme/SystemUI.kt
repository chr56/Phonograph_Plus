/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.theme

import util.theme.activity.setTaskDescriptionColor
import util.theme.color.darkenColor
import android.app.Activity

fun Activity.updateTaskDescriptionColor(color: Int = darkenColor(primaryColor())) = setTaskDescriptionColor(color)
