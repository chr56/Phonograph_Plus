/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.viewcreater

import player.phonograph.R
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.ButtonBarLayout
import androidx.core.util.valueIterator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.util.SparseArray
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.FrameLayout.LayoutParams as FrameLayoutLayoutParams
import android.widget.LinearLayout.LayoutParams as LinearLayoutLayoutParams


fun buildDialogView(
    context: Context,
    titlePanel: TitlePanel?,
    contentPanel: ContentPanel,
    buttonPanel: ButtonPanel,
): ViewGroup {

    fun panelLayoutParams(horizontal: Int = 36, vertical: Int = 48) =
        ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(horizontal, vertical, horizontal, vertical)
        }

    val frameLayout = FrameLayout(context).also { frameLayout ->

        val contentScrollView = ScrollView(context).also { scrollView ->
            scrollView.addView(contentPanel.panel, panelLayoutParams())
        }

        frameLayout.addView(contentScrollView,
            FrameLayoutLayoutParams(panelLayoutParams()).also {
                it.gravity = Gravity.CENTER
                it.setMargins(
                    48,
                    if (titlePanel != null) 160 else 64,
                    48,
                    160
                )
            }
        )

        if (titlePanel != null) frameLayout.addView(
            titlePanel.panel,
            FrameLayoutLayoutParams(panelLayoutParams()).also {
                it.gravity = Gravity.TOP
            }
        )

        frameLayout.addView(
            buttonPanel.panel,
            FrameLayoutLayoutParams(panelLayoutParams(vertical = 16)).also {
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
    val contentPanel = FrameLayout(context).apply(constructor)
    return ContentPanel(contentPanel)
}

private fun createButton(context: Context, buttonText: String, color: Int, onClick: (View) -> Unit): Button {
    return AppCompatButton(context).apply {
        text = buttonText
        textSize = 14f
        isSingleLine = true
        gravity = Gravity.CENTER
        setPadding(12, 0, 12, 0)
        minWidth = 64
        background = ColorDrawable(Color.TRANSPARENT)
        setTextColor(color)
        setOnClickListener { onClick(it) }
    }
}