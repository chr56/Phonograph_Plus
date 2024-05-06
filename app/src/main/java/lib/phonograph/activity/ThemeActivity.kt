/*
 * Copyright (c) 2022~2024 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package lib.phonograph.activity

import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.settings.ThemeSetting
import player.phonograph.util.theme.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.PathInterpolator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * An abstract class providing material activity (no toolbar)
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class ThemeActivity : MultiLanguageActivity() {
    private var createTime: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {

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

        observeTheme()
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

    private fun observeTheme() {
        lifecycleScope.launch(Dispatchers.IO) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                Setting(this@ThemeActivity)[Keys.theme].flow.collect {
                    ThemeSetting.updateThemeStyle(this@ThemeActivity)
                    setTheme(ThemeSetting.themeStyle(this@ThemeActivity))
                    requireRecreate = true
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                primaryColorFlow(this@ThemeActivity).collect {
                    ThemeSetting.updateCachedPrimaryColor(this@ThemeActivity)
                    if (firstTime) firstTime = false else requireRecreate = true
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                accentColorFlow(this@ThemeActivity).collect {
                    ThemeSetting.updateCachedAccentColor(this@ThemeActivity)
                    if (firstTime) firstTime = false else requireRecreate = true
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (requireRecreate) {
            requireRecreate = false
            postRecreate()
        }
    }

    private var firstTime: Boolean = true // todo
    private var requireRecreate: Boolean = false
    protected fun postRecreate() {
        // hack to prevent java.lang.RuntimeException: Performing pause of activity that is not resumed
        // makes sure recreate() is called right after and not in onResume()
        Handler(Looper.getMainLooper()).post { recreate() }
    }

    //
    // System UI Colors
    //

    protected fun updateSystemUIColors(color: Int) = updateAllSystemUIColors(this, color)

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
