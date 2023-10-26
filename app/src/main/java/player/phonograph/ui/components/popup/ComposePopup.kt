/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.components.popup

import mt.util.color.resolveColor
import androidx.appcompat.R
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.PopupWindow

class ComposePopup private constructor(
    rootView: ViewGroup,
    width: Int,
    height: Int,
) : PopupWindow(rootView, width, height, true) {

    init {
        animationStyle = android.R.style.Animation_Dialog
        setBackgroundDrawable(ColorDrawable(backgroundColor(rootView.context)))
        elevation = 4f
    }

    private fun backgroundColor(context: Context): Int =
        resolveColor(
            context,
            R.attr.colorBackgroundFloating,
            context.getColor(player.phonograph.R.color.cardBackgroundColor)
        )

    companion object {
        fun content(
            context: Context,
            width: Int = WRAP_CONTENT,
            height: Int = WRAP_CONTENT,
            content: @Composable () -> Unit,
        ): ComposePopup {
            // root container
            val frameLayout = FrameLayout(context).apply {
                id = android.R.id.content
                val lifecycleOwner = context as? LifecycleOwner
                require(lifecycleOwner != null) { "$context is not a LifecycleOwner!" }
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner as? SavedStateRegistryOwner)
            }
            // actual compose view
            val composeView = ComposeView(frameLayout.context)

            frameLayout.addView(composeView, MATCH_PARENT, MATCH_PARENT)
            composeView.setContent {
                MaterialTheme {
                    content()
                }
            }

            return ComposePopup(frameLayout, width, height)
        }
    }
}