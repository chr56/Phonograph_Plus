/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.base

import android.content.Context
import androidx.annotation.MenuRes
import lib.phonograph.cab.ToolbarCab
import lib.phonograph.cab.CabStatus
import player.phonograph.R
import player.phonograph.util.PhonographColorUtil
import util.mdcolor.pref.ThemeColor

class MultiSelectionCabController(val cab: ToolbarCab) {
    fun showContent(context: Context, checkedListSize: Int, @MenuRes menuRes: Int): Boolean {
        return if (cab.status != CabStatus.STATUS_DESTROYED || cab.status != CabStatus.STATUS_DESTROYING) {
            if (checkedListSize < 1) cab.hide()

            cab.backgroundColor = PhonographColorUtil.shiftBackgroundColorForLightText(ThemeColor.primaryColor(context))
            cab.titleText = context.getString(R.string.x_selected, checkedListSize)
            cab.titleTextColor = ThemeColor.textColorPrimary(context)
            cab.menuRes = menuRes

            cab.show()
            true
        } else {
            false
        }
    }

    fun dismiss(): Boolean {
        if (cab.status == CabStatus.STATUS_ACTIVE) {
            cab.hide()
            return true
        }
        return false
    }

    fun distroy(): Boolean = cab.destroy()
}