/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.activity

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import player.phonograph.R
import player.phonograph.settings.Setting
import player.phonograph.util.Util
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer
import util.mddesign.util.ColorUtil

// todo remove Platform check

/**
 * An abstract class providing material activity (no toolbar)
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class ThemeActivity : AppCompatActivity() {
    private var createTime: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createTime = System.currentTimeMillis()
        setTheme(Setting.instance.generalTheme)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
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
        Handler().post { recreate() }
    }

    //
    // User Interface
    //
    protected open fun setDrawUnderStatusbar() {
        Util.setAllowDrawUnderStatusBar(window)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) Util.setAllowDrawUnderStatusBar(window)
//        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) Util.setStatusBarTranslucent(window)
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
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        val statusBar = window.decorView.rootView.findViewById<View>(R.id.status_bar)
        if (statusBar != null) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            statusBar.setBackgroundColor(ColorUtil.darkenColor(color))
//                } else {
//                    statusBar.setBackgroundColor(color)
//                }
        } else /* if (Build.VERSION.SDK_INT >= 21) */ {
            window.statusBarColor = ColorUtil.darkenColor(color)
        }
        setLightStatusbarAuto(color)
//        }
    }
    open fun setStatusbarColorAuto() {
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
    open fun setNavigationbarColorAuto() {
        setNavigationbarColor(ThemeColor.navigationBarColor(this))
    }

    //
    // Task Description
    //
    open fun setTaskDescriptionColor(@ColorInt color: Int) {
        Themer.setTaskDescriptionColor(this, color)
    }
    open fun setTaskDescriptionColorAuto() {
        setTaskDescriptionColor(ThemeColor.primaryColor(this))
    }

}
