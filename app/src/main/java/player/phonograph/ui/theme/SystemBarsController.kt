/*
 *  Copyright (c) 2024~2026 chr_56
 */

@file:SuppressLint("ObsoleteSdkInt")

package player.phonograph.ui.theme

import util.theme.color.isColorLight
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.view.View
import android.view.Window
import android.view.WindowManager

/////////////////////////////////////////////////


interface SystemBarsController {
    /**
     * set up windows and cutoff behavior to enable Edge-To-Edge.
     */
    fun enableEdgeToEdge(window: Window, view: View)

    /**
     * Update system-bars behaviors and colors
     * @param statusBarColor new status bar color to be applied
     * @param navigationBarColor new navigation bar color to be applied
     * @param nightMode current night mode (dark color scheme)
     */
    fun updateSystemBars(
        window: Window, view: View, stubs: SystemBarsStubs,
        @ColorInt statusBarColor: Int, @ColorInt navigationBarColor: Int, nightMode: Boolean,
    )

}

fun createSystemBarsController(): SystemBarsController =
    when {
        SDK_INT >= 35 -> SystemBarsControllerEdgeToEdge()
        SDK_INT >= 30 -> SystemBarsControllerApi30()
        SDK_INT >= 29 -> SystemBarsControllerApi29()
        SDK_INT >= 28 -> SystemBarsControllerApi28()
        SDK_INT >= 26 -> SystemBarsControllerApi26()
        SDK_INT >= 23 -> SystemBarsControllerApi23()
        SDK_INT >= 21 -> SystemBarsControllerApi21()
        else          -> SystemBarsControllerBase()
    }

open class SystemBarsControllerBase : SystemBarsController {
    override fun enableEdgeToEdge(window: Window, view: View) {}

    override fun updateSystemBars(
        window: Window, view: View, stubs: SystemBarsStubs,
        statusBarColor: Int, navigationBarColor: Int, nightMode: Boolean,
    ) {
    }


    /**
     * Make window fit system windows, so that it can draw behind system-bars, enable edge to edge.
     */
    protected fun stretchToEdge(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    /**
     * determine to enable light foreground by color and nightMode
     */
    protected fun shouldEnableLightForeground(@ColorInt color: Int, nightMode: Boolean): Boolean =
        if (Color.alpha(color) <= 80) { // translucent: base on background
            !nightMode
        } else { // colored
            isColorLight(color)
        }
}

@RequiresApi(21)
open class SystemBarsControllerApi21 : SystemBarsControllerBase() {
    override fun enableEdgeToEdge(window: Window, view: View) {
        stretchToEdge(window)
    }

    @Suppress("DEPRECATION")
    override fun updateSystemBars(
        window: Window,
        view: View,
        stubs: SystemBarsStubs,
        statusBarColor: Int,
        navigationBarColor: Int,
        nightMode: Boolean,
    ) {
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    }
}

@RequiresApi(23)
open class SystemBarsControllerApi23 : SystemBarsControllerApi21() {
    override fun enableEdgeToEdge(window: Window, view: View) {
        stretchToEdge(window)
    }

    @Suppress("DEPRECATION")
    override fun updateSystemBars(
        window: Window, view: View, stubs: SystemBarsStubs,
        statusBarColor: Int, navigationBarColor: Int,
        nightMode: Boolean,
    ) {
        window.statusBarColor = statusBarColor
        window.navigationBarColor = if (nightMode) navigationBarColorDark else navigationBarColorLight
        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = shouldEnableLightForeground(statusBarColor, nightMode)
        }
    }

    private val navigationBarColorLight = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
    private val navigationBarColorDark = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
}

@RequiresApi(26)
open class SystemBarsControllerApi26 : SystemBarsControllerApi23() {

    @Suppress("DEPRECATION")
    override fun updateSystemBars(
        window: Window, view: View, stubs: SystemBarsStubs,
        statusBarColor: Int, navigationBarColor: Int,
        nightMode: Boolean,
    ) {
        window.statusBarColor = statusBarColor
        window.navigationBarColor = navigationBarColor
        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = shouldEnableLightForeground(statusBarColor, nightMode)
            isAppearanceLightNavigationBars = shouldEnableLightForeground(navigationBarColor, nightMode)
        }
    }
}

@RequiresApi(28)
open class SystemBarsControllerApi28 : SystemBarsControllerApi26() {
    override fun enableEdgeToEdge(window: Window, view: View) {
        stretchToEdge(window)
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
}

@RequiresApi(29)
open class SystemBarsControllerApi29 : SystemBarsControllerApi28() {

    @Suppress("DEPRECATION")
    override fun updateSystemBars(
        window: Window, view: View, stubs: SystemBarsStubs,
        statusBarColor: Int, navigationBarColor: Int,
        nightMode: Boolean,
    ) {
        window.statusBarColor = statusBarColor
        window.navigationBarColor = navigationBarColor

        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false

        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = shouldEnableLightForeground(statusBarColor, nightMode)
            isAppearanceLightNavigationBars = shouldEnableLightForeground(navigationBarColor, nightMode)
        }
    }

}

@RequiresApi(30)
open class SystemBarsControllerApi30 : SystemBarsControllerApi29() {
    override fun enableEdgeToEdge(window: Window, view: View) {
        stretchToEdge(window)
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }
}

@RequiresApi(30)
open class SystemBarsControllerEdgeToEdge : SystemBarsControllerApi30() {
    @Suppress("DEPRECATION")
    override fun enableEdgeToEdge(window: Window, view: View) {
        stretchToEdge(window)
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

        // Disable vanilla drawing
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
    }

    override fun updateSystemBars(
        window: Window, view: View, stubs: SystemBarsStubs,
        statusBarColor: Int, navigationBarColor: Int,
        nightMode: Boolean,
    ) {
        with(stubs) {
            statusbar(view)?.setBackgroundColor(statusBarColor)
            statusbarOverlay(view)?.setBackgroundColor(statusBarColor)
            navigationbar(view)?.setBackgroundColor(navigationBarColor)
        }

        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = shouldEnableLightForeground(statusBarColor, nightMode)
            isAppearanceLightNavigationBars = shouldEnableLightForeground(navigationBarColor, nightMode)
        }
    }
}
