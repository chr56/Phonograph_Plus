/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.theme

import player.phonograph.mechanism.setting.StyleConfig
import android.content.Context

val Context.nightMode: Boolean get() = StyleConfig.isNightMode(this)