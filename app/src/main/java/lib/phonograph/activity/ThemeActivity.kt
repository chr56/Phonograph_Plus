/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.activity

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import player.phonograph.R
import player.phonograph.settings.Setting
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer

/**
 * An abstract class providing material activity (no toolbar)
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class ThemeActivity : AppCompatActivity() {
    private var createTime: Long = -1

    protected var primaryColor: Int = 0
    protected var accentColor: Int = 0
    protected var textColorPrimary: Int = 0
    protected var textColorSecondary: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        updateColor()
        ThemeColor.mPreferences(this).registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)

        super.onCreate(savedInstanceState)
        createTime = System.currentTimeMillis()

        // theme
        setTheme(Setting.instance.generalTheme)

        // night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        // immersive status bar
        if (useCustomStatusBar) setFullScreenAndIncludeStatusBar()

        // color
        if (autoSetStatusBarColor) setStatusbarColorAuto()
        if (autoSetNavigationBarColor) setNavigationbarColorAuto()
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

    private fun updateColor() {
        primaryColor = ThemeColor.primaryColor(this)
        accentColor = ThemeColor.accentColor(this)
        textColorPrimary = ThemeColor.textColorPrimary(this)
        textColorSecondary = ThemeColor.textColorSecondary(this)
    }

    private val onSharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener {
            _: SharedPreferences, key: String ->
        when (key) {
            ThemeColor.KEY_PRIMARY_COLOR -> primaryColor = ThemeColor.primaryColor(this)
            ThemeColor.KEY_ACCENT_COLOR -> primaryColor = ThemeColor.accentColor(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Themer.didThemeValuesChange(this, createTime)) {
            postRecreate()
        }
    }

    protected fun postRecreate() {
        // hack to prevent java.lang.RuntimeException: Performing pause of activity that is not resumed
        // makes sure recreate() is called right after and not in onResume()
        Handler(Looper.getMainLooper()).post { recreate() }
    }

    override fun onDestroy() {
        super.onDestroy()
        ThemeColor.mPreferences(this).unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
    }

    protected fun updateAllColors(color: Int) {
        setStatusbarColor(color)
        setNavigationbarColor(color)
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
     * This will set the color of the view with the id "status_bar" on KitKat and Lollipop.
     * On Lollipop if no such view is found it will set the statusbar color using the native method.
     *
     * @param color the new statusbar color (will be shifted down on Lollipop and above)
     */
    open fun setStatusbarColor(color: Int) {
        val darkColor = ColorUtil.darkenColor(color)
        window.decorView.rootView.findViewById<View>(R.id.status_bar)?.setBackgroundColor(darkColor)
        window.statusBarColor = darkColor
        setLightStatusbarAuto(color)
    }
    private fun setStatusbarColorAuto() {
        // we don't want to use statusbar color because we are doing the color darkening on our own to support KitKat
        setStatusbarColor(ThemeColor.primaryColor(this))
    }
    open fun setLightStatusbar(enabled: Boolean) {
        Themer.setLightStatusbar(this, enabled)
    }

    open fun setLightStatusbarAuto(bgColor: Int) {
        setLightStatusbar(ColorUtil.isColorLight(bgColor))
    }

    //
    // NavigationBar
    //
    open fun setNavigationbarColor(color: Int) {
        if (ThemeColor.coloredNavigationBar(this)) {
            Themer.setNavigationbarColor(this, color)
        } else {
            Themer.setNavigationbarColor(this, Color.BLACK)
        }
    }
    private fun setNavigationbarColorAuto() {
        setNavigationbarColor(ThemeColor.navigationBarColor(this))
    }

    //
    // Task Description
    //
    open fun setTaskDescriptionColor(@ColorInt color: Int) {
        Themer.setTaskDescriptionColor(this, color)
    }
    private fun setTaskDescriptionColorAuto() {
        setTaskDescriptionColor(ThemeColor.primaryColor(this))
    }

    //
    // SnackBar holder
    //
    protected open val snackBarContainer: View get() = window.decorView
}
