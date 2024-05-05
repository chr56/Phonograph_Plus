/*
 * Copyright (c) 2022~2024 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.activity

import player.phonograph.settings.ThemeSetting
import player.phonograph.settings.ThemeSetting.accentColor
import player.phonograph.settings.ThemeSetting.primaryColor
import player.phonograph.util.theme.nightMode
import player.phonograph.util.theme.restoreNotFullsScreen
import player.phonograph.util.theme.setFullScreenAndIncludeStatusBar
import player.phonograph.util.theme.updateNavigationbarColor
import player.phonograph.util.theme.updateStatusbarColor
import player.phonograph.util.theme.updateTaskDescriptionColor
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import androidx.lifecycle.lifecycleScope
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.PathInterpolator
import kotlinx.coroutines.launch

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
        observeColors()

        super.onCreate(savedInstanceState)
        createTime = System.currentTimeMillis()

        // theme
        setTheme(ThemeSetting.themeStyle(this))

        // immersive status bar
        if (fullScreen) updateFullScreenSettings()

        // color
        if (autoSetStatusBarColor) updateStatusbarColor()
        if (autoSetNavigationBarColor) updateNavigationbarColor()
        if (autoSetTaskDescriptionColor) updateTaskDescriptionColor()
    }

    /** Must call before super */
    protected var fullScreen: Boolean = true
        set(value) {
            field = value
            updateFullScreenSettings()
        }

    /** Must call before super */
    protected var autoSetStatusBarColor: Boolean = true

    /** Must call before super */
    protected var autoSetNavigationBarColor: Boolean = true

    /** Must call before super */
    protected var autoSetTaskDescriptionColor: Boolean = true

    private fun observeColors() {
        primaryColor = primaryColor(this)
        accentColor = accentColor(this)
        lifecycleScope.launch {
            ThemeSetting.observeColors(this@ThemeActivity) { primary, accent ->
                primaryColor = primary
                accentColor = accent
            }
        }
        textColorPrimary = primaryTextColor(nightMode)
        textColorSecondary = secondaryTextColor(nightMode)
    }


    override fun onResume() {
        super.onResume()
        if (requireRecreate) {
            postRecreate()
            requireRecreate = false
        }
    }

    private var requireRecreate: Boolean = false
    protected fun postRecreate() {
        // hack to prevent java.lang.RuntimeException: Performing pause of activity that is not resumed
        // makes sure recreate() is called right after and not in onResume()
        Handler(Looper.getMainLooper()).post { recreate() }
    }

    //
    // System UI Colors
    //

    protected fun updateSystemUIColors(color: Int) {
        updateStatusbarColor(color)
        updateNavigationbarColor(color)
        updateTaskDescriptionColor(color)
    }

    //
    // User Interface
    //

    protected fun updateFullScreenSettings() {
        if (fullScreen) setFullScreenAndIncludeStatusBar() else restoreNotFullsScreen()
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
        oldColor: Int, newColor: Int, action: (ValueAnimator) -> Unit,
    ) { // todo: make sure lifecycle
        colorChangeAnimator?.end()
        colorChangeAnimator?.cancel()
        colorChangeAnimator = ValueAnimator
            .ofArgb(oldColor, newColor)
            .setDuration(600L)
            .also { animator ->
                animator.interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)
                animator.addUpdateListener(action)
                animator.start()
            }
    }

    protected fun cancelThemeColorChange() {
        colorChangeAnimator?.end()
        colorChangeAnimator?.cancel()
        colorChangeAnimator = null
    }
}
