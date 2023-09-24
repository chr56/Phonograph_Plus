/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.components.popup

import mt.pref.ThemeColor
import mt.util.color.resolveColor
import mt.util.color.secondaryTextColor
import player.phonograph.util.theme.nightMode
import androidx.appcompat.R
import androidx.viewbinding.ViewBinding
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow

abstract class OptionsPopup protected constructor(
    viewBinding: ViewBinding,
    width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
) : PopupWindow(viewBinding.root, width, height, true) {

    init {
        animationStyle = android.R.style.Animation_Dialog
        super.setBackgroundDrawable(ColorDrawable(backgroundColor(viewBinding.root.context)))
    }

    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        super.showAtLocation(parent, gravity, x, y)
        onShow()
    }

    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int, gravity: Int) {
        super.showAsDropDown(anchor, xoff, yoff, gravity)
        onShow()
    }

    protected open fun onShow() {}

    protected fun backgroundColor(context: Context): Int =
        resolveColor(
            context,
            R.attr.colorBackgroundFloating,
            context.getColor(player.phonograph.R.color.cardBackgroundColor)
        )


    protected var accentColor: Int = 0
        private set
    protected var textColor: Int = 0
        private set
    protected var widgetColor: ColorStateList = ColorStateList.valueOf(0)
        private set

    protected fun prepareColors(context: Context) {
        accentColor = ThemeColor.accentColor(context)
        textColor = context.secondaryTextColor(context.nightMode)
        widgetColor = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_selected),
                intArrayOf()
            ),
            intArrayOf(accentColor, accentColor, textColor)
        )
    }
}