/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.setting

import player.phonograph.R
import androidx.annotation.StringDef
import androidx.annotation.StyleRes
import android.content.Context
import android.content.SharedPreferences

object StyleConfig {

    private const val PREFERENCE_NAME = "style_config"
    private fun sharedPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    @GeneralTheme
    fun generalTheme(context: Context): String =
        sharedPreferences(context).getString(KEY_THEME, THEME_AUTO) ?: THEME_AUTO

    fun setGeneralTheme(context: Context, @GeneralTheme theme: String) {
        sharedPreferences(context).edit().putString(KEY_THEME, theme).apply()
    }

    @StyleRes
    private fun parseToStyleRes(themePrefValue: String?): Int =
        when (themePrefValue) {
            THEME_AUTO  -> R.style.Theme_Phonograph_Auto
            THEME_DARK  -> R.style.Theme_Phonograph_Dark
            THEME_BLACK -> R.style.Theme_Phonograph_Black
            THEME_LIGHT -> R.style.Theme_Phonograph_Light
            else        -> R.style.Theme_Phonograph_Auto
        }

    @StyleRes
    fun generalThemeStyle(context: Context): Int =
        parseToStyleRes(generalTheme(context))


    fun toggleTheme(context: Context): Boolean {
        val themeSetting = generalThemeStyle(context)
        return if (themeSetting != R.style.Theme_Phonograph_Auto) {
            when (themeSetting) {
                R.style.Theme_Phonograph_Light                                -> setGeneralTheme(context, THEME_DARK)
                R.style.Theme_Phonograph_Dark, R.style.Theme_Phonograph_Black -> setGeneralTheme(context, THEME_LIGHT)
            }
            true
        } else {
            false
        }
    }

    private const val KEY_THEME = "theme"

    const val THEME_AUTO = "auto"
    const val THEME_DARK = "dark"
    const val THEME_BLACK = "black"
    const val THEME_LIGHT = "light"


    val values get() = listOf(THEME_AUTO, THEME_LIGHT, THEME_DARK, THEME_BLACK)
    fun names(context: Context) = listOf(
        R.string.auto_theme_name,
        R.string.light_theme_name,
        R.string.dark_theme_name,
        R.string.black_theme_name,
    ).map {
        context.getString(it)
    }

    @StringDef(THEME_AUTO, THEME_DARK, THEME_BLACK, THEME_LIGHT)
    @Retention(AnnotationRetention.SOURCE)
    annotation class GeneralTheme
}