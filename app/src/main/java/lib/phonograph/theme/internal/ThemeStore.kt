/*
 *  Copyright (c) 2022~2024 chr_56
 */

package lib.phonograph.theme.internal

import lib.phonograph.theme.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.AttrRes
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import lib.phonograph.theme.internal.MonetColor.Depth
import lib.phonograph.theme.internal.MonetColor.MonetColorPalette
import lib.phonograph.theme.internal.MonetColor.Type
import util.theme.internal.resolveColor

/**
 * a class to store theme color preference
 * @author Aidan Follestad (afollestad), Karim Abou Zeid (kabouzeid), che_56 (modified)
 */
@Suppress("unused")
class ThemeStore internal constructor(private val context: Context) {

    internal val pref: SharedPreferences =
        context.getSharedPreferences(CONFIG_PREFS_KEY_DEFAULT, Context.MODE_PRIVATE)

    private val mEditor = pref.edit()

    fun primaryColor(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(KEY_PRIMARY_COLOR, color)
        return this
    }

    fun primaryColorRes(@ColorRes colorRes: Int): ThemeStore {
        return primaryColor(ContextCompat.getColor(context, colorRes))
    }

    fun primaryColorAttr(@AttrRes colorAttr: Int): ThemeStore {
        return primaryColor(context.resolveColor(colorAttr, util.theme.materials.R.color.md_blue_A400))
    }

    fun accentColor(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(KEY_ACCENT_COLOR, color)
        return this
    }

    fun accentColorRes(@ColorRes colorRes: Int): ThemeStore {
        return accentColor(ContextCompat.getColor(context, colorRes))
    }

    fun accentColorAttr(@AttrRes colorAttr: Int): ThemeStore {
        return accentColor(context.resolveColor(colorAttr, util.theme.materials.R.color.md_yellow_900))
    }

    fun coloredStatusBar(colored: Boolean): ThemeStore {
        mEditor.putBoolean(KEY_APPLY_PRIMARYDARK_STATUSBAR, colored)
        return this
    }

    fun coloredNavigationBar(applyToNavBar: Boolean): ThemeStore {
        mEditor.putBoolean(KEY_APPLY_PRIMARY_NAVBAR, applyToNavBar)
        return this
    }

    fun enableMonet(enable: Boolean): ThemeStore {
        mEditor.putBoolean(KEY_ENABLE_MONET, enable)
        return this
    }

    fun preferredMonetPrimaryColor(@Type type: Int, @Depth depth: Int): ThemeStore {
        mEditor.putInt(KEY_MONET_PRIMARY_COLOR, MonetColorPalette(type, depth).value)
        return this
    }

    fun preferredMonetAccentColor(@Type type: Int, @Depth depth: Int): ThemeStore {
        mEditor.putInt(KEY_MONET_ACCENT_COLOR, MonetColorPalette(type, depth).value)
        return this
    }

    fun markChanged() = mEditor.commit().also { ThemeColor.updateCachedColor(context) }

    fun commit() =
        mEditor.putLong(VALUES_CHANGED, System.currentTimeMillis())
            .putBoolean(IS_CONFIGURED_KEY, true)
            .commit()
            .also { ThemeColor.updateCachedColor(context) }

    fun apply() =
        mEditor.putLong(VALUES_CHANGED, System.currentTimeMillis())
            .putBoolean(IS_CONFIGURED_KEY, true)
            .apply()
            .also { ThemeColor.updateCachedColor(context) }

    /**
     * **Dangerous !**, this reset all SharedPreferences!
     */
    fun clearAllPreference() {
        mEditor.clear().commit()
    }

    @CheckResult
    fun isConfigured(): Boolean {
        return pref.getBoolean(IS_CONFIGURED_KEY, false)
    }

    @SuppressLint("ApplySharedPref")
    fun isConfigured(@IntRange(from = 0, to = Int.MAX_VALUE.toLong()) version: Int): Boolean {
        val lastVersion = pref.getInt(IS_CONFIGURED_VERSION_KEY, -1)
        if (version > lastVersion) {
            mEditor.putInt(IS_CONFIGURED_VERSION_KEY, version).commit()
            return false
        }
        return true
    }

    companion object {
        internal fun edit(context: Context): ThemeStore = ThemeStore(context)

        fun isConfigured(context: Context): Boolean = ThemeStore(context).isConfigured()

        fun isConfigured(
            context: Context, @IntRange(from = 0, to = Int.MAX_VALUE.toLong()) version: Int,
        ): Boolean =
            ThemeStore(context).isConfigured(version)

        @SuppressLint("CommitPrefEdits")
        fun didThemeValuesChange(context: Context, since: Long): Boolean =
            isConfigured(context) &&
                    ThemeStore(context).pref.getLong(VALUES_CHANGED, -1) > since
    }
}
