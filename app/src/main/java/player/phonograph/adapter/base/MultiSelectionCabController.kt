/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.base

import android.content.Context
import android.graphics.Color
import android.view.MenuItem
import android.view.View
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import lib.phonograph.cab.CabStatus
import lib.phonograph.cab.ToolbarCab
import player.phonograph.R
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.PhonographColorUtil
import util.mdcolor.pref.ThemeColor

class MultiSelectionCabController(val cab: ToolbarCab) {

    var cabColor: Int = PhonographColorUtil.shiftBackgroundColorForLightText(ThemeColor.primaryColor(cab.activity))
        set(value) {
            field = value
            cab.backgroundColor = value
        }

    var textColor: Int = Color.WHITE
        set(value) {
            field = value
            cab.titleTextColor = value
        }

    fun showContent(context: Context, checkedListSize: Int, @MenuRes menuRes: Int): Boolean {
        return run {
            if (checkedListSize < 1) {
                cab.hide()
            } else {
                cab.backgroundColor = cabColor
                cab.titleText = context.getString(R.string.x_selected, checkedListSize)
                cab.titleTextColor = textColor
                cab.navigationIcon = context.getTintedDrawable(R.drawable.ic_close_white_24dp, Color.WHITE)!!

                cab.menuRes = menuRes
                cab.menuItemClickListener = Toolbar.OnMenuItemClickListener {
                    onMenuItemClick(it)
                }
                cab.closeClickListener = View.OnClickListener {
                    dismiss()
                }
                cab.show()
            }
            true
        }
    }

    var onMenuItemClick: (MenuItem) -> Boolean = { false }
    var onDismiss: () -> Unit = {}
    fun dismiss(): Boolean {
        if (cab.status == CabStatus.STATUS_ACTIVE) {
            cab.hide()
            onDismiss()
            return true
        }
        return false
    }

    fun isActive(): Boolean = cab.status == CabStatus.STATUS_ACTIVE
}
