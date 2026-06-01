/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.util

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType
import android.view.ViewGroup

/**
 * apply Edge-to-edge window insets of SystemBars for this ViewGroup that are at bottom
 * @param what insets type that used
 */
fun ViewGroup.applyWindowInsetsAsBottomView(
    @InsetsType what: Int = WindowInsetsCompat.Type.systemBars(),
) {
    if (clipToPadding) clipToPadding = false
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(what)
        view.setPadding(
            view.paddingLeft,
            view.paddingTop,
            view.paddingRight,
            insets.bottom
        )
        windowInsets
    }
}

/**
 * apply Edge-to-edge window insets of SystemBars for this ViewGroup that are at bottom, but controllable
 * @param what insets type that used
 * @param enable initial state
 * @return a controller that could control WindowInsets flexibly
 */
fun ViewGroup.applyControllableWindowInsetsAsBottomView(
    @InsetsType what: Int = WindowInsetsCompat.Type.systemBars(),
    enable: Boolean = true,
): BottomViewWindowInsetsController {
    if (clipToPadding) clipToPadding = false
    return BottomViewWindowInsetsControllerImpl(this, enable).apply {
        trackWindowInsetsChanges(what)
    }
}

/**
 * control bottom WindowInsets padding of a view that is at bottom
 */
interface BottomViewWindowInsetsController {

    var enabled: Boolean

    /**
     * add WindowInsets bottom padding
     */
    fun applyWindowInsets()

    /**
     * resume bottom padding to original
     */
    fun cancelWindowInsets()
}

private class BottomViewWindowInsetsControllerImpl(
    val target: ViewGroup,
    enabled: Boolean,
) : BottomViewWindowInsetsController {


    private var windowsInsetsBottom: Int = -1
    private var originalPaddingBottom: Int = -1

    private val paddingBottomOriginal: Int
        get() = originalPaddingBottom.coerceAtLeast(0)
    private val paddingBottomWithInsets: Int
        get() = if (windowsInsetsBottom > 0) windowsInsetsBottom + paddingBottomOriginal else paddingBottomOriginal

    override var enabled: Boolean = enabled
        set(value) {
            field = value
            if (value) {
                applyWindowInsets()
            } else {
                cancelWindowInsets()
            }
        }

    override fun applyWindowInsets() {
        setPaddingBottom(paddingBottomWithInsets)
    }

    override fun cancelWindowInsets() {
        setPaddingBottom(paddingBottomOriginal)
    }

    fun trackWindowInsetsChanges(@InsetsType type: Int) {
        ViewCompat.setOnApplyWindowInsetsListener(target) { view, windowInsets ->
            if (originalPaddingBottom < 0) originalPaddingBottom = view.paddingBottom
            val insets = windowInsets.getInsets(type)
            windowsInsetsBottom = insets.bottom
            if (enabled) applyWindowInsets()
            windowInsets
        }
    }

    private fun setPaddingBottom(paddingBottom: Int) {
        target.setPadding(
            target.paddingLeft,
            target.paddingTop,
            target.paddingRight,
            paddingBottom
        )
    }

}