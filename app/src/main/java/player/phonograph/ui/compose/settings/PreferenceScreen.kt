/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.ui.compose.settings

import com.alorma.compose.settings.storage.base.SettingValueState
import com.alorma.compose.settings.storage.base.rememberBooleanSettingState
import com.alorma.compose.settings.storage.base.rememberIntSettingState
import com.alorma.compose.settings.storage.datastore.rememberPreferenceDataStoreBooleanSettingState
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsListDropdown
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import lib.phonograph.localization.LanguageSettingDialog
import player.phonograph.R
import player.phonograph.mechanism.setting.HomeTabConfig
import player.phonograph.mechanism.setting.StyleConfig.THEME_AUTO
import player.phonograph.mechanism.setting.StyleConfig.THEME_BLACK
import player.phonograph.mechanism.setting.StyleConfig.THEME_DARK
import player.phonograph.mechanism.setting.StyleConfig.THEME_LIGHT
import player.phonograph.settings.*
import player.phonograph.ui.dialogs.ClickModeSettingDialog
import player.phonograph.ui.dialogs.HomeTabConfigDialog
import player.phonograph.ui.dialogs.ImageSourceConfigDialog
import player.phonograph.ui.dialogs.NowPlayingScreenPreferenceDialog
import player.phonograph.ui.dialogs.PathFilterDialog
import player.phonograph.util.NavigationUtil
import player.phonograph.util.reportError
import player.phonograph.util.warning
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun PhonographPreferenceScreen() {
    Column(
        Modifier.verticalScroll(rememberScrollState())
    ) {

        SettingsGroup(title = header(R.string.pref_header_appearance)) {

            ListPref( //todo
                titleRes = R.string.pref_title_general_theme,
                optionGroup =
                OptionGroup(
                    key = GENERAL_THEME,
                    optionsValue = listOf(
                        THEME_AUTO,
                        THEME_DARK,
                        THEME_BLACK,
                        THEME_LIGHT,
                    ),
                    optionsStringRes = listOf(
                        R.string.auto_theme_name,
                        R.string.dark_theme_name,
                        R.string.black_theme_name,
                        R.string.light_theme_name,
                    )
                )
            )

            DialogPref(
                dialog = LanguageSettingDialog::class.java,
                titleRes = R.string.app_language,
                //todo
            )
        }

        SettingsGroup(title = header(R.string.pref_header_library)) {
            DialogPref(
                dialog = HomeTabConfigDialog::class.java,
                titleRes = R.string.library_categories,
                summaryRes = R.string.pref_summary_library_categories,
            )
            val context = LocalContext.current
            SettingsMenuLink(
                title = title(R.string.pref_title_reset_home_pages_tab_config),
                subtitle = subtitle(R.string.pref_summary_reset_home_pages_tab_config)
            ) {
                AlertDialog.Builder(context)
                    .setTitle(R.string.pref_title_reset_home_pages_tab_config)
                    .setMessage(
                        "${context.getString(R.string.pref_summary_reset_home_pages_tab_config)}\n" +
                                "${context.getString(R.string.are_you_sure)}\n"
                    )
                    .setPositiveButton(android.R.string.ok) { _, _ -> HomeTabConfig.resetHomeTabConfig() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
            }
            BooleanPref(
                key = REMEMBER_LAST_TAB,
                titleRes = R.string.pref_title_remember_last_tab,
                summaryRes = R.string.pref_summary_remember_last_tab,
            )
            BooleanPref(
                key = FIXED_TAB_LAYOUT,
                titleRes = R.string.perf_title_fixed_tab_layout,
                summaryRes = R.string.pref_summary_fixed_tab_layout,
            )
        }

        SettingsGroup(title = header(R.string.path_filter)) {
            DialogPref(
                dialog = PathFilterDialog::class.java,
                titleRes = R.string.path_filter,
                //todo
            )
        }

        SettingsGroup(
            title = header(R.string.pref_header_notification)
        ) {
            BooleanPref(
                key = CLASSIC_NOTIFICATION,
                titleRes = R.string.pref_title_classic_notification,
                summaryRes = R.string.pref_summary_classic_notification
            )
            BooleanPref(
                key = COLORED_NOTIFICATION,
                titleRes = R.string.pref_title_colored_notification,
                summaryRes = R.string.pref_summary_colored_notification,
                enabled = dependOn(CLASSIC_NOTIFICATION)
            )
        }

        SettingsGroup(
            title = header(R.string.pref_header_now_playing_screen)
        ) {
            DialogPref(
                dialog = NowPlayingScreenPreferenceDialog::class.java,
                titleRes = R.string.pref_title_now_playing_screen_appearance,
            )
            BooleanPref(
                key = DISPLAY_LYRICS_TIME_AXIS,
                titleRes = R.string.pref_title_display_lyrics_time_axis,
                summaryRes = R.string.pref_summary_display_lyrics_time_axis,
            )
            BooleanPref(
                key = SYNCHRONIZED_LYRICS_SHOW,
                titleRes = R.string.pref_title_synchronized_lyrics_show,
                summaryRes = R.string.pref_summary_synchronized_lyrics_show,
            )

        }

        SettingsGroup(
            title = header(R.string.pref_header_images)
        ) {
            DialogPref(
                dialog = ImageSourceConfigDialog::class.java,
                titleRes = R.string.image_source_config,
            )
            ListPref(
                titleRes = R.string.pref_title_auto_download_metadata,
                optionGroup = OptionGroup(
                    AUTO_DOWNLOAD_IMAGES_POLICY,
                    listOf(
                        DOWNLOAD_IMAGES_POLICY_NEVER,
                        DOWNLOAD_IMAGES_POLICY_ONLY_WIFI,
                        DOWNLOAD_IMAGES_POLICY_ALWAYS,
                    ),
                    listOf(
                        R.string.never,
                        R.string.only_on_wifi,
                        R.string.always,
                    )
                )
            )
        }

        SettingsGroup(
            title = header(R.string.pref_header_player_behaviour)
        ) {
            DialogPref(
                dialog = ClickModeSettingDialog::class.java,
                titleRes = R.string.pref_title_click_behavior,
                summaryRes = R.string.pref_summary_click_behavior,
            )
            BooleanPref(
                key = AUDIO_DUCKING,
                summaryRes = R.string.pref_summary_audio_ducking,
                titleRes = R.string.pref_title_audio_ducking,
            )
            BooleanPref(
                key = GAPLESS_PLAYBACK,
                summaryRes = R.string.pref_summary_gapless_playback,
                titleRes = R.string.pref_title_gapless_playback,
            )
            BooleanPref(
                key = ENABLE_LYRICS,
                summaryRes = R.string.pref_summary_load_lyrics,
                titleRes = R.string.pref_title_load_lyrics,
            )
            BooleanPref(
                key = BROADCAST_SYNCHRONIZED_LYRICS,
                summaryRes = R.string.pref_summary_send_lyrics,
                titleRes = R.string.pref_title_send_lyrics,
            )
            BooleanPref(
                key = BROADCAST_CURRENT_PLAYER_STATE,
                summaryRes = R.string.pref_summary_broadcast_current_player_state,
                titleRes = R.string.pref_title_broadcast_current_player_state,
            )
            EqualizerSetting()
        }


        SettingsGroup(title = header(R.string.pref_header_playlists)) {
            ListPref(
                titleRes = R.string.pref_title_last_added_interval,
                optionGroup =
                OptionGroup(
                    LAST_ADDED_CUTOFF,
                    listOf(
                        INTERVAL_TODAY,
                        INTERVAL_PAST_SEVEN_DAYS,
                        INTERVAL_PAST_FOURTEEN_DAYS,
                        INTERVAL_PAST_ONE_MONTH,
                        INTERVAL_PAST_THREE_MONTHS,
                        INTERVAL_THIS_WEEK,
                        INTERVAL_THIS_MONTH,
                        INTERVAL_THIS_YEAR,
                    ),
                    listOf(
                        R.string.today,
                        R.string.past_seven_days,
                        R.string.past_fourteen_days,
                        R.string.past_one_month,
                        R.string.past_three_months,
                        R.string.this_week,
                        R.string.this_month,
                        R.string.this_year,
                    )
                )
            )
        }

        SettingsGroup(title = header(R.string.check_upgrade)) {
            BooleanPref(
                key = CHECK_UPGRADE_AT_STARTUP,
                titleRes = R.string.auto_check_upgrade,
                summaryRes = R.string.auto_check_upgrade_summary,
            )
        }

        SettingsGroup(title = header(R.string.pref_header_compatibility)) {
            BooleanPref(
                key = USE_LEGACY_FAVORITE_PLAYLIST_IMPL,
                titleRes = R.string.pref_title_use_legacy_favorite_playlist_impl,
                summaryRes = R.string.pref_summary_use_legacy_favorite_playlist_impl,
            )
            BooleanPref(
                key = USE_LEGACY_LIST_FILES_IMPL,
                titleRes = R.string.use_legacy_list_Files,
            )
            BooleanPref(
                key = USE_LEGACY_DETAIL_DIALOG,
                titleRes = R.string.pref_title_use_legacy_detail_dialog,
                summaryRes = R.string.pref_summary_use_legacy_detail_dialog,
            )
            ListPref(
                titleRes = R.string.pref_title_playlist_files_operation_behaviour,
                summaryRes = R.string.pref_summary_playlist_files_operation_behaviour,
                optionGroup = OptionGroup(
                    PLAYLIST_FILES_OPERATION_BEHAVIOUR,
                    listOf(
                        PLAYLIST_OPS_BEHAVIOUR_AUTO,
                        PLAYLIST_OPS_BEHAVIOUR_FORCE_SAF,
                        PLAYLIST_OPS_BEHAVIOUR_FORCE_LEGACY,
                    ),
                    listOf(
                        R.string.behaviour_auto,
                        R.string.behaviour_force_saf,
                        R.string.behaviour_force_legacy,
                    )
                )
            )
        }

    }
}

@Composable
private fun EqualizerSetting() {
    val activity = if (!LocalInspectionMode.current) LocalContext.current as? Activity else null
    SettingsMenuLink(title = title(R.string.equalizer)) {
        if (activity != null) {
            NavigationUtil.openEqualizer(activity)
        } else {
            warning("Equalizer", "can not open Equalizer Setting")
        }
    }
}


@Composable
private fun BooleanPref(
    key: String,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int = 0,
    enabled: Boolean = true,
    defaultValue: Boolean = false,
) {
    val booleanState =
        if (LocalInspectionMode.current) {
            rememberBooleanSettingState(true)
        } else {
            rememberPreferenceDataStoreBooleanSettingState(
                key = key,
                dataStore = LocalContext.current.dataStore,
                defaultValue = defaultValue
            )
        }
    SettingsSwitch(
        state = booleanState,
        enabled = enabled,
        title = title(titleRes),
        subtitle = subtitle(summaryRes),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DialogPref(
    dialog: Class<out DialogFragment>,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int = 0,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    SettingsMenuLink(
        enabled = enabled,
        title = title(titleRes),
        subtitle = subtitle(summaryRes),
    ) {
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity != null) {
            try {
                val fragmentManager = fragmentActivity.supportFragmentManager
                dialog.getConstructor().newInstance().show(fragmentManager, dialog.simpleName)
            } catch (e: Exception) {
                reportError(e, "DialogPref", "Failed to show dialog ${dialog.name}")
            }
        } else {
            warning("DialogPref", "$context can not show dialog")
        }
    }
}


@Composable
private fun ListPref(
    optionGroup: OptionGroup,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int = 0,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val state = rememberIntSettingState(0)
    if (!LocalInspectionMode.current) {
        LaunchedEffect(key1 = optionGroup.key) {
            val selected = optionGroup.selected(context)
            if (selected > -1) {
                state.value = optionGroup.selected(context)
            } else {
                warning("ListPref", "can not read preference ${optionGroup.key}")
            }
        }
    }
    val items =
        if (LocalInspectionMode.current) {
            optionGroup.optionsValue
        } else {
            optionGroup.options(context)
        }
    val onItemSelected: (Int, String) -> Unit =
        if (LocalInspectionMode.current) {
            { _, _ -> }
        } else {
            { index, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    optionGroup.onSelect(context, index)
                }
            }
        }

    ListPrefImpl(
        state = state,
        items = items,
        onItemSelected = onItemSelected,
        titleRes = titleRes
    )
}

@Composable
private fun ListPrefImpl(
    items: List<String>,
    state: SettingValueState<Int>,
    onItemSelected: (Int, String) -> Unit,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int = 0,
    enabled: Boolean = true,
) {
    SettingsListDropdown(
        state = state,
        enabled = enabled,
        title = title(titleRes),
        subtitle = subtitle(summaryRes),
        items = items,
        onItemSelected = onItemSelected
    )
}

internal class OptionGroup(
    val key: String,
    val optionsValue: List<String>,
    val optionsStringRes: List<Int>,
) {

    fun options(context: Context): List<String> {
        return optionsStringRes.map { context.getString(it) }
    }

    suspend fun selected(context: Context): Int {
        val value = context.dataStore.data.first()[stringPreferencesKey(key)]
        return optionsValue.indexOf(value)
    }

    suspend fun onSelect(context: Context, index: Int) {
        context.dataStore.edit { preferences ->
            val newValue = optionsValue.getOrElse(index) { optionsValue.first() }
            preferences[stringPreferencesKey(key)] = newValue
        }
    }
}


@Composable
private fun dependOn(key: String, required: Boolean = true): Boolean {
    return if (LocalInspectionMode.current) {
        false
    } else {
        val datastore = LocalContext.current.dataStore
        rememberPreferenceDataStoreBooleanSettingState(key = key, dataStore = datastore).value == required
    }
}

//region Text
private fun header(res: Int) = @Composable {
    Text(text = stringResource(id = res), Modifier.padding(start = 16.dp))
}

private fun title(res: Int) = @Composable {
    BoxWithConstraints {
        Text(
            text = stringResource(id = res),
            modifier = Modifier.widthIn(max = maxWidth * 4 / 5)
        )
    }
}

private fun subtitle(res: Int): (@Composable () -> Unit)? =
    if (res != 0) {
        @Composable {
            BoxWithConstraints {
                Text(
                    text = stringResource(id = res),
                    modifier = Modifier.widthIn(max = maxWidth * 4 / 5)
                )
            }
        }
    } else {
        null
    }
//endregion
