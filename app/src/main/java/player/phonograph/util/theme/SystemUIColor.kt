/*
 *  Copyright (c) 2022~2024 chr_56
 */

@file:SuppressLint("ObsoleteSdkInt")

package player.phonograph.util.theme

import util.theme.color.isColorLight
import player.phonograph.R
import util.theme.activity.setTaskDescriptionColor
import util.theme.color.darkenColor
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.view.View
import android.view.Window
import android.view.WindowManager

private var Impl: SystemUIModifier? = null

val systemUIModifier: SystemUIModifier
    get() =
        Impl ?: if (SDK_INT >= 35) {
            SystemUIModifierApi35()
        } else if (SDK_INT >= 30) {
            SystemUIModifierApi30()
        } else if (SDK_INT >= 29) {
            SystemUIModifierApi29()
        } else if (SDK_INT >= 28) {
            SystemUIModifierApi28()
        } else if (SDK_INT >= 26) {
            SystemUIModifierApi26()
        } else if (SDK_INT >= 23) {
            SystemUIModifierApi23()
        } else if (SDK_INT >= 21) {
            SystemUIModifierApi21()
        } else {
            SystemUIModifierBase()
        }.also { Impl = it }

fun Activity.setupSystemBars() {
    systemUIModifier.setUp(window, window.decorView)
}

fun Activity.updateSystemBarsColor(@ColorInt statusBarColor: Int, @ColorInt navigationBarColor: Int) {
    systemUIModifier.updateSystemBars(window, window.decorView, statusBarColor, navigationBarColor, nightMode)
}

fun Activity.updateTaskDescriptionColor(color: Int = darkenColor(primaryColor())) = setTaskDescriptionColor(color)

interface SystemUIModifier {
    fun setUp(window: Window, view: View)
    fun updateSystemBars(
        window: Window, view: View,
        @ColorInt statusBarColor: Int, @ColorInt navigationBarColor: Int, nightMode: Boolean,
    )
}

private open class SystemUIModifierBase : SystemUIModifier {
    override fun setUp(window: Window, view: View) {}
    override fun updateSystemBars(
        window: Window,
        view: View,
        statusBarColor: Int,
        navigationBarColor: Int,
        nightMode: Boolean,
    ) {
    }

    protected fun shouldEnableLightFrontGround(@ColorInt color: Int, nightMode: Boolean): Boolean =
        if (Color.alpha(color) <= 80) { // translucent: base on background
            !nightMode
        } else { // colored
            isColorLight(color)
        }
}

@RequiresApi(21)
private open class SystemUIModifierApi21 : SystemUIModifierBase() {
    @Suppress("DEPRECATION")
    override fun setUp(window: Window, view: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    }
}

@RequiresApi(23)
private open class SystemUIModifierApi23 : SystemUIModifierApi21() {
    override fun setUp(window: Window, view: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    @Suppress("DEPRECATION")
    override fun updateSystemBars(
        window: Window, view: View,
        statusBarColor: Int, navigationBarColor: Int,
        nightMode: Boolean,
    ) {
        window.statusBarColor = statusBarColor
        window.navigationBarColor = if (nightMode) navigationBarColorDark else navigationBarColorLight
        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = shouldEnableLightFrontGround(statusBarColor, nightMode)
        }
    }

    private val navigationBarColorLight = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
    private val navigationBarColorDark = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
}

@RequiresApi(26)
private open class SystemUIModifierApi26 : SystemUIModifierApi23() {

    @Suppress("DEPRECATION")
    override fun updateSystemBars(
        window: Window, view: View,
        statusBarColor: Int, navigationBarColor: Int,
        nightMode: Boolean,
    ) {
        window.statusBarColor = statusBarColor
        window.navigationBarColor = navigationBarColor
        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = shouldEnableLightFrontGround(statusBarColor, nightMode)
            isAppearanceLightNavigationBars = shouldEnableLightFrontGround(navigationBarColor, nightMode)
        }
    }
}

@RequiresApi(28)
private open class SystemUIModifierApi28 : SystemUIModifierApi26() {
    override fun setUp(window: Window, view: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
}

@RequiresApi(29)
private open class SystemUIModifierApi29 : SystemUIModifierApi28() {

    @Suppress("DEPRECATION")
    override fun updateSystemBars(
        window: Window, view: View,
        statusBarColor: Int, navigationBarColor: Int,
        nightMode: Boolean,
    ) {
        window.statusBarColor = statusBarColor
        window.navigationBarColor = navigationBarColor

        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false

        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = shouldEnableLightFrontGround(statusBarColor, nightMode)
            isAppearanceLightNavigationBars = shouldEnableLightFrontGround(navigationBarColor, nightMode)
        }
    }

}

@RequiresApi(30)
private open class SystemUIModifierApi30 : SystemUIModifierApi29() {
    override fun setUp(window: Window, view: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }
}

@RequiresApi(35)
private open class SystemUIModifierApi35 : SystemUIModifierApi30() {

    override fun setUp(window: Window, view: View) {
        super.setUp(window, view)
        makeSureEdgeToEdge(window) // Enforced Edge-to-Edge enforced only when targeting 35
    }

    @Suppress("DEPRECATION")
    private fun makeSureEdgeToEdge(window: Window) {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
    }


    override fun updateSystemBars(
        window: Window, view: View,
        statusBarColor: Int, navigationBarColor: Int,
        nightMode: Boolean,
    ) {
        statusbar(view)?.setBackgroundColor(statusBarColor)
        navigationbar(view)?.setBackgroundColor(navigationBarColor)

        WindowInsetsControllerCompat(window, view).run {
            isAppearanceLightStatusBars = shouldEnableLightFrontGround(statusBarColor, nightMode)
            isAppearanceLightNavigationBars = shouldEnableLightFrontGround(navigationBarColor, nightMode)
        }
    }

    private fun statusbar(view: View): View? = view.findViewById<View>(R.id.status_bar)
    private fun navigationbar(view: View): View? = view.findViewById<View>(R.id.navigation_bar)
}