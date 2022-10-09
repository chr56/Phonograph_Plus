/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams as LinearLayoutLayoutParams
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.ButtonBarLayout
import player.phonograph.R


fun buildDialogView(context: Context, titlePanel: TitlePanel, contentPanel: ContentPanel, buttonPanel: ButtonPanel): ScrollView {
    val scrollView = ScrollView(context).apply {
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(titlePanel.panel, LinearLayoutLayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(contentPanel.panel, LinearLayoutLayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(buttonPanel.panel, LinearLayoutLayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        addView(linearLayout)
    }
    return scrollView
}

class TitlePanel(val panel: FrameLayout, val titleView: TextView)

internal fun titlePanel(context: Context): TitlePanel {
    val titleView = TextView(context).apply {
        textSize = 20f
        setTextColor(context.getColor(R.color.dialog_button_color))
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    val titlePanel = FrameLayout(context).apply {
        setPadding(24, 36, 24, 36)
        addView(titleView)
    }
    return TitlePanel(titlePanel, titleView)
}

class ButtonPanel(val panel: ButtonBarLayout)

@SuppressLint("RestrictedApi")
internal fun buttonPanel(
    context: Context,
    constructor: ButtonBarLayout.() -> Unit,
): ButtonPanel {
    val buttonPanel = ButtonBarLayout(context, null).apply(constructor)
    return ButtonPanel(buttonPanel)
}

class ContentPanel(val panel: FrameLayout)

internal fun contentPanel(context: Context, constructor: FrameLayout.() -> Unit): ContentPanel {
    val contentPanel = FrameLayout(context)
        .apply {
            setPadding(24, 16, 24, 16)
        }.apply(constructor)
    return ContentPanel(contentPanel)
}