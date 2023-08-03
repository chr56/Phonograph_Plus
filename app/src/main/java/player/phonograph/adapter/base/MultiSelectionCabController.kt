/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.base

import lib.phonograph.cab.ToolbarCab
import lib.phonograph.cab.ToolbarCab.Companion.STATUS_ACTIVE
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.shiftBackgroundColorForLightText
import androidx.appcompat.widget.Toolbar
import android.graphics.Color
import android.view.View

class MultiSelectionCabController(val cab: ToolbarCab) {

    var cabColor: Int = shiftBackgroundColorForLightText(ThemeColor.primaryColor(cab.activity))
        set(value) {
            field = value
            cab.backgroundColor = value
        }

    var textColor: Int = Color.WHITE
        set(value) {
            field = value
            cab.titleTextColor = value
        }

    fun showContent(checkedListSize: Int): Boolean {
        return run {
            if (checkedListSize < 1) {
                cab.hide()
            } else {
                cab.backgroundColor = cabColor
                cab.titleText = cab.toolbar.resources.getString(R.string.x_selected, checkedListSize)
                cab.titleTextColor = textColor
                cab.navigationIcon = cab.activity.getTintedDrawable(R.drawable.ic_close_white_24dp, Color.WHITE)!!

                if (hasMenu) cab.menuHandler = menuHandler
                cab.closeClickListener = View.OnClickListener {
                    dismiss()
                }
                cab.show()
            }
            true
        }
    }

    var menuHandler: ((Toolbar) -> Boolean)? = null

    private val hasMenu get() = menuHandler != null

    var onDismiss: () -> Unit = {}
    fun dismiss(): Boolean {
        if (cab.status == STATUS_ACTIVE) {
            cab.hide()
            onDismiss()
            return true
        }
        return false
    }

    fun isActive(): Boolean = cab.status == STATUS_ACTIVE

}
