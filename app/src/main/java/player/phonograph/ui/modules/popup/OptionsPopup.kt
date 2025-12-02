/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.popup

import player.phonograph.util.theme.ThemeSettingsDelegate.textColorSecondary
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.themeFloatingBackgroundColor
import player.phonograph.util.ui.convertDpToPixel
import androidx.viewbinding.ViewBinding
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
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
        elevation = convertDpToPixel(4f, resources)
        animationStyle = android.R.style.Animation_Dialog
        super.setBackgroundDrawable(ColorDrawable(themeFloatingBackgroundColor(viewBinding.root.context)))
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

    protected var accentColor: Int = 0
        private set
    protected var textColor: Int = 0
        private set
    protected var widgetColor: ColorStateList = ColorStateList.valueOf(0)
        private set

    protected fun prepareColors(context: Context) {

        accentColor = context.accentColor()
        textColor = textColorSecondary(context)
        widgetColor = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_selected),
                intArrayOf()
            ),
            intArrayOf(accentColor, accentColor, textColor)
        )
    }

    protected val resources: Resources get() = contentView.resources
}