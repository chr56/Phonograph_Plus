/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */
package lib.phonograph.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import lib.phonograph.view.BorderCircleView
import player.phonograph.R

/**
 * @author Aidan Follestad (afollestad)
 */
class ColorPreferenceX : Preference {

    constructor(context: Context?) : this(context, null, 0) {
        init(context, null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0) {
        init(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context?, attrs: AttributeSet?) {
        layoutResource = R.layout.x_preference
        widgetLayoutResource = R.layout.x_preference_color
        isPersistent = false
    }

    private var mView: View? = null

    private var color = 0
    private var border = 0

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        mView = holder.itemView
        invalidateColor()
    }

    fun setColor(color: Int, border: Int) {
        this.color = color
        this.border = border
        invalidateColor()
    }

    private fun invalidateColor() {
        mView?.findViewById<BorderCircleView>(R.id.circle)?.apply {
            if (color != 0) {
                visibility = View.VISIBLE
                setBackgroundColor(color)
                borderColor = border
            } else {
                visibility = View.GONE
            }
        }
    }
}
