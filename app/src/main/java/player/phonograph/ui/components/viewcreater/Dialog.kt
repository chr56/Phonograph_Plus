/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.viewcreater

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import androidx.appcompat.widget.ButtonBarLayout
import androidx.core.util.valueIterator
import player.phonograph.R
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout.LayoutParams as LinearLayoutLayoutParams
import android.widget.FrameLayout.LayoutParams as FrameLayoutLayoutParams


fun buildDialogView(
    context: Context,
    titlePanel: TitlePanel?,
    contentPanel: ContentPanel,
    buttonPanel: ButtonPanel,
): ViewGroup {

    val defaultLayoutParams =
        ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(16, 8, 16, 8)
        }

    val frameLayout = FrameLayout(context).also { frameLayout ->

        val contetScrollView = ScrollView(context).also { scrollView ->
            scrollView.addView(contentPanel.panel, defaultLayoutParams)
        }

        frameLayout.addView(contetScrollView, FrameLayoutLayoutParams(defaultLayoutParams).also {
            it.gravity = Gravity.CENTER
            it.setMargins(8, if (titlePanel != null) 160 else 48, 8, 160)
        })

        if (titlePanel != null) frameLayout.addView(
            titlePanel.panel,
            FrameLayoutLayoutParams(defaultLayoutParams).also {
                it.gravity = Gravity.TOP
            }
        )

        frameLayout.addView(
            buttonPanel.panel,
            FrameLayoutLayoutParams(defaultLayoutParams).also {
                it.gravity = Gravity.BOTTOM
            }
        )
    }

    return frameLayout
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

@SuppressLint("RestrictedApi")
class ButtonPanel(val panel: ButtonBarLayout, val buttons: SparseArray<View> = SparseArray(3)) {
    class Builder(private val context: Context) {

        private val buttons: SparseArray<View> = SparseArray(2)
        var orientation: Int = LinearLayout.HORIZONTAL

        fun button(index: Int, buttonText: String, color: Int, onClick: (View) -> Unit): Builder {
            buttons.put(index, createButton(context, buttonText, color, onClick))
            return this
        }

        fun space(index: Int): Builder {
            buttons.put(index, Space(context))
            return this
        }

        fun build(): ButtonPanel {
            val panel = ButtonBarLayout(context, null)
            val buttonLayoutParams = LinearLayoutLayoutParams(WRAP_CONTENT, WRAP_CONTENT, 0f)
            val spaceLayoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1f)

            panel.orientation = this.orientation

            //todo
            for (button in buttons.valueIterator()) {
                panel.addView(
                    button,
                    if (button is Space) spaceLayoutParams else buttonLayoutParams
                )
            }

            return ButtonPanel(panel, buttons)
        }
    }
}


internal fun buttonPanel(context: Context, constructor: ButtonPanel.Builder.() -> Unit): ButtonPanel {
    return ButtonPanel.Builder(context).apply(constructor).build()
}

class ContentPanel(val panel: FrameLayout)

internal fun contentPanel(context: Context, constructor: FrameLayout.() -> Unit): ContentPanel {
    val contentPanel = FrameLayout(context)
        .apply {
            setPadding(24, 16, 24, 16)
        }.apply(constructor)
    return ContentPanel(contentPanel)
}