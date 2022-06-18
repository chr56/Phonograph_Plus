/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RadioButton
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.util.PhonographColorUtil
import util.mdcolor.pref.ThemeColor

class ListOptionsPopup private constructor(
    private val context: Context,
    val viewBinding: PopupWindowMainBinding,
    width: Int,
    height: Int,
) : PopupWindow(viewBinding.root, width, height, true) {

    var onDismiss: (PopupWindowMainBinding) -> Unit = { }
    var onShow: (PopupWindowMainBinding) -> Unit = { }

    constructor(
        context: Context,
        width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    ) : this(
        context,
        PopupWindowMainBinding.inflate(LayoutInflater.from(context)),
        width, height
    )

    init {
        // background
        setBackgroundDrawable(
            ColorDrawable(PhonographColorUtil.getCorrectBackgroundColor(context))
        )
        // animate
        this.animationStyle = android.R.style.Animation_Dialog
        setUpColor()
    }

    override fun dismiss() {
        super.dismiss()
        onDismiss(viewBinding)
    }

    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        super.showAtLocation(parent, gravity, x, y)
        onShow()
    }

    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int, gravity: Int) {
        super.showAsDropDown(anchor, xoff, yoff, gravity)
        onShow()
    }

    private fun onShow() {
        hideAllPopupItems()
        onShow(viewBinding)
        setBackgroundDrawable(
            ColorDrawable(PhonographColorUtil.getCorrectBackgroundColor(context))
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun hideAllPopupItems() {
        viewBinding.apply {
            sortOrderBasic.visibility = View.GONE
            sortOrderBasic.clearCheck()
            textSortOrderBasic.visibility = View.GONE

            sortOrderContent.visibility = View.GONE
            sortOrderContent.clearCheck()
            textSortOrderContent.visibility = View.GONE

            textGridSize.visibility = View.GONE
            gridSize.clearCheck()
            gridSize.visibility = View.GONE

            actionColoredFooters.visibility = View.GONE
        }
    }

    /**
     * widget color
     */
    private fun setUpColor() {
        // color
        val accentColor = ThemeColor.accentColor(context)
        val textColor = ThemeColor.textColorSecondary(context)
        val widgetColor = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_selected),
                intArrayOf()
            ),
            intArrayOf(accentColor, accentColor, textColor)
        )
        viewBinding.apply {
            // text color
            this.textGridSize.setTextColor(accentColor)
            this.textSortOrderBasic.setTextColor(accentColor)
            this.textSortOrderContent.setTextColor(accentColor)
            // checkbox color
            this.actionColoredFooters.buttonTintList = widgetColor
            // radioButton
            for (i in 0 until this.gridSize.childCount) (this.gridSize.getChildAt(i) as RadioButton).buttonTintList =
                widgetColor
            for (i in 0 until this.sortOrderContent.childCount) (this.sortOrderContent.getChildAt(i) as RadioButton).buttonTintList =
                widgetColor
            for (i in 0 until this.sortOrderBasic.childCount) (this.sortOrderBasic.getChildAt(i) as RadioButton).buttonTintList =
                widgetColor
        }
    }
}
