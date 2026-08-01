/*
 *  Copyright (c) 2024~2026 chr_56
 */

package player.phonograph.ui.theme

import player.phonograph.foundation.compat.buildTaskDescription
import player.phonograph.settings.Keys
import player.phonograph.settings.Settings
import util.theme.color.darkenColor
import util.theme.color.stripAlpha
import androidx.annotation.ColorInt
import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import java.util.WeakHashMap

object SystemBarsControllerDelegate {

    fun enableEdgeToEdge(activity: Activity) {
        controller(activity).enableEdgeToEdge(activity.window, activity.window.decorView)
    }

    fun register(
        activity: Activity,
        statusbar: View?, statusbarViewId: Int,
        statusbarOverlay: View?, statusbarOverlayViewId: Int,
        navigationbar: View?, navigationbarViewId: Int,
    ) {
        stubs(activity).registerViews(
            statusbar, statusbarViewId, statusbarOverlay, statusbarOverlayViewId, navigationbar, navigationbarViewId
        )
    }

    fun unregister(activity: Activity) {
        synchronized(stubs) {
            stubs.remove(activity)?.unregisterViews()
        }
    }

    fun updateSystemBarsColor(
        activity: Activity,
        @ColorInt statusBarColor: Int,
        @ColorInt navigationBarColor: Int,
        nightMode: Boolean = ThemeSettingsDelegate.isNightTheme(activity.resources),
    ) {
        controller(activity).updateSystemBars(
            activity.window,
            activity.window.decorView,
            stubs(activity),
            statusBarColor,
            navigationBarColor,
            nightMode
        )
    }

    fun updateTaskDescriptionColor(activity: Activity, color: Int = darkenColor(ThemeSettingsDelegate.primaryColor())) {
        activity.setTaskDescription(
            buildTaskDescription(stripAlpha(color), activity.title.toString())
        )
    }

    private val stubs = WeakHashMap<Activity, SystemBarsStubs>()

    private fun stubs(activity: Activity): SystemBarsStubs =
        synchronized(stubs) {
            stubs.getOrPut(activity, ::SystemBarsStubs)
        }

    private var systemBarsController: SystemBarsController? = null
    private var lifecycleCallbacksRegistered = false

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            unregister(activity)
        }
    }

    private fun controller(activity: Activity): SystemBarsController {
        ensureLifecycleCallbacks(activity)
        return systemBarsController ?: synchronized(this) {
            createSystemBarsController(
                force = Settings(activity)[Keys.forceEnableEdgeToEdge].data
            ).also { systemBarsController = it }
        }
    }

    private fun ensureLifecycleCallbacks(activity: Activity) {
        if (!lifecycleCallbacksRegistered) {
            activity.application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
            lifecycleCallbacksRegistered = true
        }
    }
}
