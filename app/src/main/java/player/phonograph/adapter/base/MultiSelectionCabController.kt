/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.base

import lib.phonograph.cab.ToolbarCab
import lib.phonograph.cab.ToolbarCab.Companion.STATUS_ACTIVE
import lib.phonograph.cab.createToolbarCab
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.shiftBackgroundColorForLightText
import androidx.appcompat.widget.Toolbar
import android.app.Activity
import android.graphics.Color
import android.view.View

class MultiSelectionCabController private constructor(val cab: ToolbarCab) {

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

    fun prepare() {
        cab.backgroundColor = cabColor
        cab.titleText = cab.toolbar.resources.getString(R.string.x_selected, 0)
        cab.titleTextColor = textColor
        cab.navigationIcon = cab.activity.getTintedDrawable(R.drawable.ic_close_white_24dp, Color.WHITE)!!

        if (hasMenu) cab.menuHandler = menuHandler
        cab.closeClickListener = View.OnClickListener {
            dismiss()
        }
    }

    /**
     * @param size selected size
     */
    fun updateCab(size: Int) {
        updateCountText(size)
        if (size > 0) {
            if (hasMenu) cab.menuHandler = menuHandler
            cab.show()
        } else {
            cab.hide()
        }
    }

    /**
     * @param size selected size
     */
    private fun updateCountText(size: Int) {
        cab.titleText = cab.toolbar.resources.getString(R.string.x_selected, size)
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

    companion object {
        fun create(activity: Activity): MultiSelectionCabController {
            val cab = createToolbarCab(activity, R.id.cab_stub, R.id.multi_selection_cab)
            return MultiSelectionCabController(cab).also { it.prepare() }
        }
    }
}
