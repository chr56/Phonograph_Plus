/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.viewcreater

import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.ColorDrawable
import android.view.Gravity.CENTER
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.AppCompatButton

fun createButton(context: Context, buttonText: String, color: Int, onClick: (View) -> Unit): Button {
    return AppCompatButton(context).apply {
        text = buttonText
        textSize = 14f
        isSingleLine = true
        gravity = CENTER
        setPadding(12, 0, 12, 0)
        minWidth = 64
        background = ColorDrawable(TRANSPARENT)
        setTextColor(color)
        setOnClickListener { onClick(it) }
    }
}