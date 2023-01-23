/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.activity

import mt.pref.ThemeColor
import mt.pref.ThemeColor.accentColor
import mt.pref.ThemeColor.primaryColor
import mt.pref.internal.ThemeStore.Companion.didThemeValuesChange
import mt.tint.*
import mt.util.color.darkenColor
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.util.PhonographColorUtil.nightMode
import player.phonograph.util.preferences.StyleConfig
import androidx.appcompat.app.AppCompatDelegate
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.animation.PathInterpolator

/**
 * An abstract class providing material activity (no toolbar)
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class ThemeActivity : MultiLanguageActivity() {
    private var createTime: Long = -1

    protected var primaryColor: Int = 0
    protected var accentColor: Int = 0
    protected var textColorPrimary: Int = 0
    protected var textColorSecondary: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        retrieveColors()
        ThemeColor.registerPreferenceChangeListener(listener, this.applicationContext, this)

        super.onCreate(savedInstanceState)
        createTime = System.currentTimeMillis()

        // theme
        setTheme(StyleConfig.generalTheme(this))

        // night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        // immersive status bar
        if (useCustomStatusBar) setFullScreenAndIncludeStatusBar()

        // color
        if (autoSetStatusBarColor) setStatusbarColor(primaryColor(this))
        if (autoSetNavigationBarColor) setNavigationBarColorAuto()
        if (autoSetTaskDescriptionColor) setTaskDescriptionColorAuto()
    }

    /** Must call before super */
    protected var useCustomStatusBar: Boolean = true
        set(value) {
            field = value
            if (value) setFullScreenAndIncludeStatusBar()
        }

    /** Must call before super */
    protected var autoSetStatusBarColor: Boolean = true

    /** Must call before super */
    protected var autoSetNavigationBarColor: Boolean = true

    /** Must call before super */
    protected var autoSetTaskDescriptionColor: Boolean = true

    private fun retrieveColors() {
        primaryColor = primaryColor(this)
        accentColor = accentColor(this)
        textColorPrimary = primaryTextColor(resources.nightMode)
        textColorSecondary = secondaryTextColor(resources.nightMode)
    }

    private val listener = object : ThemeColor.ThemePreferenceChangeListener {
        override fun onAccentColorChanged(newColor: Int) {
            primaryColor = newColor
        }

        override fun onPrimaryColorChanged(newColor: Int) {
            primaryColor = newColor
        }

        override fun onNavigationBarTintSettingChanged(coloredNavigationBar: Boolean) {
        }

        override fun onStatusBarTintSettingChanged(coloredStatusBar: Boolean) {
        }

    }

    override fun onResume() {
        super.onResume()
        if (didThemeValuesChange(this, createTime)) {
            postRecreate()
        }
    }

    protected fun postRecreate() {
        // hack to prevent java.lang.RuntimeException: Performing pause of activity that is not resumed
        // makes sure recreate() is called right after and not in onResume()
        Handler(Looper.getMainLooper()).post { recreate() }
    }

    protected fun updateAllColors(color: Int) {
        setStatusbarColor(color)
        setNavigationBarColor(color)
        setTaskDescriptionColor(color)
    }

    //
    // User Interface
    //
    private fun setFullScreenAndIncludeStatusBar() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            (SYSTEM_UI_FLAG_LAYOUT_STABLE or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    protected fun restoreNotFullsScreen() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility -=
            (SYSTEM_UI_FLAG_LAYOUT_STABLE or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    //
    // Status Bar
    //
    /**
     * This will set the color of the view with the id "status_bar" on Lollipop.
     * On Lollipop if no such view is found it will set the statusbar color using the native method.
     *
     * @param color the new statusbar color (will be shifted down on Lollipop and above)
     */
    open fun setStatusbarColor(color: Int) {
        val darkColor = darkenColor(color)
        setStatusbarColor(darkColor, R.id.status_bar)
        requireLightStatusbarAuto(darkColor)
    }

    //
    // SnackBar holder
    //
    protected open val snackBarContainer: View get() = window.decorView

    //
    // Animation
    //
    private var colorChangeAnimator: ValueAnimator? = null
    protected fun animateThemeColorChange(
        oldColor: Int, newColor: Int
    ) { // todo: make sure lifecycle
        colorChangeAnimator?.cancel()
        colorChangeAnimator = ValueAnimator
            .ofArgb(oldColor, newColor)
            .setDuration(600L)
        colorChangeAnimator?.also { animator ->
            animator.interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)
            animator.addUpdateListener { animation: ValueAnimator ->
                setStatusbarColor(animation.animatedValue as Int)
                setNavigationBarColor(animation.animatedValue as Int)
            }
            animator.start()
        }
    }

    protected fun cancelThemeColorChange() {
        colorChangeAnimator?.cancel()
        colorChangeAnimator = null
    }
}
