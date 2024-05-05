/*
 *  Copyright (c) 2022~2024 chr_56
 */

package lib.phonograph.theme

import lib.phonograph.misc.MonetColor.MonetColorPalette
import lib.phonograph.misc.MonetColor.defaultMonetAccentColor
import lib.phonograph.misc.MonetColor.defaultMonetPrimaryColor
import lib.phonograph.theme.ThemeColorKeys.KEY_ACCENT_COLOR
import lib.phonograph.theme.ThemeColorKeys.KEY_COLORED_NAVIGATION_BAR
import lib.phonograph.theme.ThemeColorKeys.KEY_COLORED_STATUSBAR
import lib.phonograph.theme.ThemeColorKeys.KEY_ENABLE_MONET
import lib.phonograph.theme.ThemeColorKeys.KEY_MONET_ACCENT_COLOR
import lib.phonograph.theme.ThemeColorKeys.KEY_MONET_PRIMARY_COLOR
import lib.phonograph.theme.ThemeColorKeys.KEY_PRIMARY_COLOR
import lib.phonograph.theme.internal.ThemeStore
import util.theme.color.shiftColor
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.S
import android.os.Handler
import android.os.Looper
import util.theme.materials.R as MR

/**
 * a class to access stored preference
 * @author Aidan Follestad (afollestad), Karim Abou Zeid (kabouzeid), che_56 (modified)
 */
object ThemeColor {

    fun editTheme(context: Context): ThemeStore = ThemeStore.edit(context)
    fun edit(context: Context, block: ThemeStore.() -> Unit) =
        ThemeStore.edit(context).apply(block).commit()

    fun enableMonet(context: Context): Boolean =
        ThemeStore(context).pref.getBoolean(KEY_ENABLE_MONET, false)

    @CheckResult
    @ColorInt
    fun primaryColor(context: Context): Int =
        if (cachedPrimaryColor <= 0) updateCachedPrimaryColor(context) else cachedPrimaryColor

    @CheckResult
    @ColorInt
    private fun primaryColorDark(context: Context): Int =
        shiftColor(primaryColor(context), 0.9f)

    @CheckResult
    @ColorInt
    fun accentColor(context: Context): Int =
        if (cachedAccentColor <= 0) updateCachedAccentColor(context) else cachedAccentColor

    @CheckResult
    fun coloredStatusBar(context: Context): Boolean =
        ThemeStore(context).pref.getBoolean(
            KEY_COLORED_STATUSBAR, true
        )

    @CheckResult
    fun coloredNavigationBar(context: Context): Boolean =
        ThemeStore(context).pref.getBoolean(
            KEY_COLORED_NAVIGATION_BAR, false
        )

    @CheckResult
    @ColorInt
    fun navigationBarColor(context: Context): Int =
        if (coloredNavigationBar(context)) primaryColor(context) else Color.BLACK

    @CheckResult
    @ColorInt
    fun statusBarColor(context: Context): Int =
        if (coloredStatusBar(context)) primaryColorDark(context) else Color.BLACK

    @CheckResult
    @ColorInt
    @RequiresApi(S)
    fun preferredMonetPrimaryColor(context: Context) =
        MonetColorPalette(
            ThemeStore(context).pref.getInt(KEY_MONET_PRIMARY_COLOR, defaultMonetPrimaryColor.value)
        ).color(context)

    @CheckResult
    @ColorInt
    @RequiresApi(S)
    fun preferredMonetAccentColor(context: Context) =
        MonetColorPalette(
            ThemeStore(context).pref.getInt(KEY_MONET_ACCENT_COLOR, defaultMonetAccentColor.value)
        ).color(context)

    @CheckResult
    fun didChangeSince(context: Context, time: Long): Boolean = ThemeStore.didThemeValuesChange(context, time)


    @get:ColorInt
    internal var cachedPrimaryColor: Int = -1

    @get:ColorInt
    internal var cachedAccentColor: Int = -1

    @ColorInt
    internal fun updateCachedPrimaryColor(context: Context): Int {
        val pref = ThemeStore(context).pref
        val primaryColor =
            if (SDK_INT >= S && pref.getBoolean(KEY_ENABLE_MONET, false)) {
                preferredMonetPrimaryColor(context)
            } else {
                pref.getInt(KEY_PRIMARY_COLOR, context.getColor(MR.color.md_blue_A400))
            }
        cachedPrimaryColor = primaryColor
        return primaryColor
    }

    @ColorInt
    internal fun updateCachedAccentColor(context: Context): Int {
        val pref = ThemeStore(context).pref
        val accentColor =
            if (SDK_INT >= S && pref.getBoolean(KEY_ENABLE_MONET, false)) {
                preferredMonetAccentColor(context)
            } else {
                pref.getInt(KEY_ACCENT_COLOR, context.getColor(MR.color.md_yellow_900))
            }
        cachedAccentColor = accentColor
        return accentColor
    }

    internal fun updateCachedColor(context: Context) {
        updateCachedPrimaryColor(context)
        updateCachedAccentColor(context)
    }

    fun registerPreferenceChangeListener(
        l: ThemePreferenceChangeListener,
        context: Context,
        host: LifecycleOwner,
    ) {
        host.lifecycle.addObserver(object : DefaultLifecycleObserver {
            val themeListener = l
            val listener = OnSharedPreferenceChangeListener { _, key ->
                Handler(Looper.myLooper() ?: Looper.getMainLooper()).postDelayed(
                    {
                        if (key == KEY_ENABLE_MONET) {
                            themeListener.onAccentColorChanged(accentColor(context))
                            themeListener.onPrimaryColorChanged(primaryColor(context))
                        } else {
                            when (key) {
                                KEY_ACCENT_COLOR, KEY_MONET_ACCENT_COLOR   ->
                                    themeListener.onAccentColorChanged(accentColor(context))

                                KEY_PRIMARY_COLOR, KEY_MONET_PRIMARY_COLOR ->
                                    themeListener.onPrimaryColorChanged(primaryColor(context))

                                KEY_COLORED_STATUSBAR                      ->
                                    themeListener.onStatusBarTintSettingChanged(coloredStatusBar(context))

                                KEY_COLORED_NAVIGATION_BAR                 ->
                                    themeListener.onNavigationBarTintSettingChanged(coloredNavigationBar(context))
                            }
                        }
                    },
                    500
                )
            }

            override fun onCreate(owner: LifecycleOwner) {
                ThemeStore(context).pref.registerOnSharedPreferenceChangeListener(listener)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                ThemeStore(context).pref.unregisterOnSharedPreferenceChangeListener(listener)
            }
        })
    }

    interface ThemePreferenceChangeListener {
        fun onAccentColorChanged(@ColorInt newColor: Int)
        fun onPrimaryColorChanged(@ColorInt newColor: Int)
        fun onStatusBarTintSettingChanged(coloredStatusBar: Boolean)
        fun onNavigationBarTintSettingChanged(coloredNavigationBar: Boolean)
    }


}
