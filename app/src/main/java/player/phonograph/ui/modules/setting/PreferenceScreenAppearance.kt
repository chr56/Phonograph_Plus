/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting

import lib.phonograph.localization.LanguageSettingDialog
import lib.phonograph.localization.LocalizationStore
import lib.phonograph.misc.ColorPalette
import player.phonograph.App
import player.phonograph.R
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.settings.THEME_AUTO_LIGHTBLACK
import player.phonograph.settings.THEME_AUTO_LIGHTDARK
import player.phonograph.settings.THEME_BLACK
import player.phonograph.settings.THEME_DARK
import player.phonograph.settings.THEME_LIGHT
import player.phonograph.settings.ThemeSetting
import player.phonograph.ui.modules.setting.components.BooleanPreference
import player.phonograph.ui.modules.setting.components.ColorPreference
import player.phonograph.ui.modules.setting.components.DialogPreference
import player.phonograph.ui.modules.setting.components.ListPreference
import player.phonograph.ui.modules.setting.components.SettingsGroup
import player.phonograph.ui.modules.setting.dialog.HomeTabConfigDialog
import player.phonograph.ui.modules.setting.dialog.MaterialColorPickerDialog
import player.phonograph.ui.modules.setting.dialog.MonetColorPickerDialog
import player.phonograph.ui.modules.setting.dialog.NowPlayingScreenStylePreferenceDialog
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import android.annotation.SuppressLint
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N_MR1
import android.os.Build.VERSION_CODES.S

@Composable
fun PreferenceScreenAppearance() {
    val context = LocalContext.current
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {
        SettingsGroup(titleRes = R.string.pref_header_appearance) {
            DialogPreference(
                dialog = LanguageSettingDialog::class.java,
                titleRes = R.string.app_language,
                currentValueForHint = { context ->
                    val locale = LocalizationStore.current(context)
                    locale.getDisplayName(locale)
                }
            )
            DialogPreference(
                dialog = NowPlayingScreenStylePreferenceDialog::class.java,
                titleRes = R.string.pref_title_player_style,
                summaryRes = R.string.pref_title_now_playing_screen_style,
                reset = {
                    resetPreference(it, R.string.pref_title_player_style, Keys.nowPlayingScreenStyle)
                }
            )
            DialogPreference(
                dialog = HomeTabConfigDialog::class.java,
                titleRes = R.string.library_categories,
                summaryRes = R.string.pref_summary_library_categories,
                reset = {
                    resetPreference(it, R.string.library_categories, Keys.homeTabConfig)
                }
            )
            BooleanPreference(
                key = Keys.rememberLastTab,
                titleRes = R.string.pref_title_remember_last_tab,
                summaryRes = R.string.pref_summary_remember_last_tab,
            )
            BooleanPreference(
                key = Keys.fixedTabLayout,
                titleRes = R.string.perf_title_fixed_tab_layout,
                summaryRes = R.string.pref_summary_fixed_tab_layout,
            )
        }

        SettingsGroup(titleRes = R.string.pref_header_colors) {
            if (SDK_INT >= S) BooleanPreference(
                key = Keys.enableMonet,
                titleRes = R.string.pref_title_enable_monet,
                summaryRes = R.string.pref_summary_enable_monet,
                onCheckedChange = {
                    @SuppressLint("ObsoleteSdkInt")
                    if (SDK_INT >= N_MR1) DynamicShortcutManager(App.instance).updateDynamicShortcuts()
                }
            )
            ColorPreference(
                titleRes = R.string.primary_color,
                summaryRes = R.string.primary_color_desc,
                ColorPalette.Variant.Primary
            )
            ColorPreference(
                titleRes = R.string.accent_color,
                summaryRes = R.string.accent_color_desc,
                ColorPalette.Variant.Accent
            )
            ListPreference(
                key = Keys.theme,
                optionsValues = listOf(
                    THEME_AUTO_LIGHTBLACK,
                    THEME_AUTO_LIGHTDARK,
                    THEME_LIGHT,
                    THEME_BLACK,
                    THEME_DARK,
                ),
                optionsValuesLocalized = listOf(
                    R.string.theme_name_auto_lightblack,
                    R.string.theme_name_auto_lightdark,
                    R.string.theme_name_light,
                    R.string.theme_name_black,
                    R.string.theme_name_dark,
                ),
                titleRes = R.string.pref_title_general_theme,
                onChange = { _, _ ->
                    ThemeSetting.updateThemeStyle(context)
                }
            )
            if (SDK_INT >= N_MR1) BooleanPreference(
                key = Keys.coloredAppShortcuts,
                titleRes = R.string.pref_title_app_shortcuts,
                summaryRes = R.string.pref_summary_colored_app_shortcuts,
                onCheckedChange = {
                    @SuppressLint("ObsoleteSdkInt")
                    if (SDK_INT >= N_MR1) DynamicShortcutManager(App.instance).updateDynamicShortcuts()
                }
            )
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
}


@Composable
private fun ColorPreference(
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    variant: ColorPalette.Variant,
) {
    val color = when (variant) {
        ColorPalette.Variant.Primary -> MaterialTheme.colors.primary
        ColorPalette.Variant.Accent  -> MaterialTheme.colors.secondary
    }
    val context = LocalContext.current
    ColorPreference(titleRes, summaryRes, color) {
        if (SDK_INT >= S && Setting(context)[Keys.enableMonet].data) {
            MonetColorPickerDialog.showColorChooserDialog(context, variant)
        } else {
            MaterialColorPickerDialog.showColorChooserDialog(context, color.toArgb(), variant)
        }
    }
}

