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
import lib.phonograph.localization.LocalizationStore
import lib.phonograph.misc.ColorChooser
import lib.phonograph.misc.ColorPalette
import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.mechanism.setting.HomeTabConfig
import player.phonograph.mechanism.setting.NowPlayingScreenConfig
import player.phonograph.mechanism.setting.StyleConfig
import player.phonograph.mechanism.setting.StyleConfig.THEME_AUTO
import player.phonograph.settings.*
import player.phonograph.ui.compose.components.ColorCircle
import player.phonograph.ui.dialogs.ClickModeSettingDialog
import player.phonograph.ui.dialogs.HomeTabConfigDialog
import player.phonograph.ui.dialogs.ImageSourceConfigDialog
import player.phonograph.ui.dialogs.MonetColorPickerDialog
import player.phonograph.ui.dialogs.NowPlayingScreenPreferenceDialog
import player.phonograph.ui.dialogs.PathFilterDialog
import player.phonograph.util.NavigationUtil
import player.phonograph.util.reportError
import player.phonograph.util.warning
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import android.app.Activity
import android.content.Context
import android.content.DialogInterface.OnDismissListener
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.N
import android.os.Build.VERSION_CODES.N_MR1
import android.os.Build.VERSION_CODES.S
import android.util.Log
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
            DialogPref(
                model = DialogPreferenceModel(
                    dialog = LanguageSettingDialog::class.java,
                    titleRes = R.string.app_language,
                    currentValueForHint = { context ->
                        val locale = LocalizationStore.current(context)
                        locale.getDisplayName(locale)
                    }
                )
            )
            DialogPref(
                model = DialogPreferenceModel(
                    dialog = NowPlayingScreenPreferenceDialog::class.java,
                    titleRes = R.string.pref_title_player_style,
                    currentValueForHint = { context ->
                        context.getString(NowPlayingScreenConfig.nowPlayingScreen.titleRes)
                    }
                ))
            LibraryCategoriesSetting()
            BooleanPref(
                key = REMEMBER_LAST_TAB,
                titleRes = R.string.pref_title_remember_last_tab,
                summaryRes = R.string.pref_summary_remember_last_tab,
                defaultValue = true,
            )
            BooleanPref(
                key = FIXED_TAB_LAYOUT,
                titleRes = R.string.perf_title_fixed_tab_layout,
                summaryRes = R.string.pref_summary_fixed_tab_layout,
                defaultValue = false,
            )
        }

        SettingsGroup(title = header(R.string.pref_header_colors)) {
            if (SDK_INT >= S) MonetSetting()
            PrimaryColorPref()
            AccentColorPref()
            ColoredNavigationBarSetting()
            if (SDK_INT >= N_MR1) {
                BooleanPref(
                    key = COLORED_APP_SHORTCUTS,
                    titleRes = R.string.pref_title_app_shortcuts,
                    summaryRes = R.string.pref_summary_colored_app_shortcuts,
                    defaultValue = true,
                    onCheckedChange = {
                        DynamicShortcutManager(App.instance).updateDynamicShortcuts()
                    }
                )
            }
            GeneralThemeSetting()
        }

        SettingsGroup(title = header(R.string.pref_header_content)) {
            DialogPref(
                model = DialogPreferenceModel(
                    dialog = PathFilterDialog::class.java,
                    titleRes = R.string.path_filter,
                    currentValueForHint = { context ->
                        with(context) {
                            if (Setting.instance.pathFilterExcludeMode) {
                                "${getString(R.string.path_filter_excluded_mode)} - \n${getString(R.string.pref_summary_path_filter_excluded_mode)}"
                            } else {
                                "${getString(R.string.path_filter_included_mode)} - \n${getString(R.string.pref_summary_path_filter_included_mode)}"
                            }
                        }
                    }
                )
            )
            ListPref(
                titleRes = R.string.pref_title_last_added_interval,
                options =
                OptionGroupModel(
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
                    ),
                    defaultValueIndex = 1
                )
            )
            DialogPref(
                model = DialogPreferenceModel(
                    dialog = ClickModeSettingDialog::class.java,
                    titleRes = R.string.pref_title_click_behavior,
                    summaryRes = R.string.pref_summary_click_behavior,
                )
            )
            DialogPref(
                model = DialogPreferenceModel(
                    dialog = ImageSourceConfigDialog::class.java,
                    titleRes = R.string.image_source_config,
                )
            )
            ListPref(
                titleRes = R.string.pref_title_auto_download_metadata,
                options = OptionGroupModel(
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
            BooleanPref(
                key = AUDIO_DUCKING,
                summaryRes = R.string.pref_summary_audio_ducking,
                titleRes = R.string.pref_title_audio_ducking,
                defaultValue = true,
            )
            BooleanPref(
                key = GAPLESS_PLAYBACK,
                summaryRes = R.string.pref_summary_gapless_playback,
                titleRes = R.string.pref_title_gapless_playback,
                defaultValue = false,
            )
            BooleanPref(
                key = BROADCAST_CURRENT_PLAYER_STATE,
                summaryRes = R.string.pref_summary_broadcast_current_player_state,
                titleRes = R.string.pref_title_broadcast_current_player_state,
                defaultValue = true,
            )
            EqualizerSetting()
        }


        SettingsGroup(
            title = header(R.string.pref_header_notification)
        ) {
            // noinspection ObsoleteSdkInt
            if (SDK_INT >= N) BooleanPref(
                key = CLASSIC_NOTIFICATION,
                titleRes = R.string.pref_title_classic_notification,
                summaryRes = R.string.pref_summary_classic_notification,
                defaultValue = false,
            )
            BooleanPref(
                key = COLORED_NOTIFICATION,
                titleRes = R.string.pref_title_colored_notification,
                summaryRes = R.string.pref_summary_colored_notification,
                defaultValue = true,
                enabled = dependOn(CLASSIC_NOTIFICATION),
            )
        }


        SettingsGroup(title = header(R.string.pref_header_lyrics)) {
            BooleanPref(
                key = ENABLE_LYRICS,
                titleRes = R.string.pref_title_load_lyrics,
                summaryRes = R.string.pref_summary_load_lyrics,
                defaultValue = true,
            )
            BooleanPref(
                key = SYNCHRONIZED_LYRICS_SHOW,
                titleRes = R.string.pref_title_synchronized_lyrics_show,
                summaryRes = R.string.pref_summary_synchronized_lyrics_show,
                defaultValue = true,
                onCheckedChange = { newValue ->
                    if (!newValue) {
                        // clear lyrics displaying on the status bar now
                        StatusBarLyric.stopLyric()
                    }
                }
            )
            BooleanPref(
                key = DISPLAY_LYRICS_TIME_AXIS,
                titleRes = R.string.pref_title_display_lyrics_time_axis,
                summaryRes = R.string.pref_summary_display_lyrics_time_axis,
                defaultValue = true,
            )
            BooleanPref(
                key = DISPLAY_LYRICS_TIME_AXIS,
                titleRes = R.string.pref_title_send_lyrics,
                summaryRes = R.string.pref_summary_send_lyrics,
                defaultValue = false,
            )
        }

        SettingsGroup(title = header(R.string.pref_header_compatibility)) {
            BooleanPref(
                key = USE_LEGACY_FAVORITE_PLAYLIST_IMPL,
                titleRes = R.string.pref_title_use_legacy_favorite_playlist_impl,
                summaryRes = R.string.pref_summary_use_legacy_favorite_playlist_impl,
                defaultValue = false,
            )
            BooleanPref(
                key = USE_LEGACY_LIST_FILES_IMPL,
                titleRes = R.string.use_legacy_list_Files,
                defaultValue = false,
            )
            BooleanPref(
                key = USE_LEGACY_STATUS_BAR_LYRICS_API,
                titleRes = R.string.pref_title_use_legacy_status_bar_lyrics_api,
                summaryRes = R.string.pref_summary_use_legacy_status_bar_lyrics_api,
                defaultValue = true,
            )
            BooleanPref(
                key = USE_LEGACY_DETAIL_DIALOG,
                titleRes = R.string.pref_title_use_legacy_detail_dialog,
                summaryRes = R.string.pref_summary_use_legacy_detail_dialog,
                defaultValue = false,
            )
            ListPref(
                titleRes = R.string.pref_title_playlist_files_operation_behaviour,
                summaryRes = R.string.pref_summary_playlist_files_operation_behaviour,
                options = OptionGroupModel(
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

        SettingsGroup(title = header(R.string.check_upgrade)) {
            BooleanPref(
                key = CHECK_UPGRADE_AT_STARTUP,
                titleRes = R.string.auto_check_upgrade,
                summaryRes = R.string.auto_check_upgrade_summary,
                defaultValue = false,
            )
        }

    }
}

//region Special Preferences

@Composable
private fun LibraryCategoriesSetting() {
    val context = LocalContext.current
    SettingsMenuLink(
        title = title(R.string.library_categories),
        subtitle = subtitle(R.string.pref_summary_library_categories),
        onClick = {
            showDialog(context, HomeTabConfigDialog::class.java, null)
        },
        action = {
            IconButton(
                onClick = {
                    AlertDialog.Builder(context)
                        .setTitle(R.string.pref_title_reset_home_pages_tab_config)
                        .setMessage(
                            "${context.getString(R.string.pref_summary_reset_home_pages_tab_config)}\n" +
                                    "${context.getString(R.string.are_you_sure)}\n"
                        )
                        .setPositiveButton(android.R.string.ok) { _, _ -> HomeTabConfig.resetHomeTabConfig() }
                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                        .show()
                },
                content = {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(id = R.string.pref_title_reset_home_pages_tab_config)
                    )
                }
            )
        }
    )
}


@Composable
private fun GeneralThemeSetting() {

    class GeneralThemeState(val context: Context) : SettingValueState<Int> {

        override var value: Int
            get() = StyleConfig.values.indexOf(StyleConfig.generalTheme(context))
            set(value) {
                StyleConfig.setGeneralTheme(context, StyleConfig.values[value])
            }

        override fun reset() {
            StyleConfig.setGeneralTheme(context, THEME_AUTO)
        }
    }

    val context = LocalContext.current

    ListPrefImpl(
        titleRes = R.string.pref_title_general_theme,
        items = remember { StyleConfig.names(context) },
        state = GeneralThemeState(context),
        onItemSelected = { _, _ ->
            (context as? Activity)?.recreate()
        }
    )
}

@Composable
private fun PrimaryColorPref() {
    val context = LocalContext.current
    val mode = remember {
        if (SDK_INT >= S && ThemeColor.enableMonet(context)) ColorPalette.MODE_MONET_PRIMARY_COLOR
        else ColorPalette.MODE_PRIMARY_COLOR
    }
    ColorPrefImpl(
        titleRes = R.string.primary_color,
        summaryRes = R.string.primary_color_desc,
        mode = mode
    )
}
@Composable
private fun AccentColorPref() {
    val context = LocalContext.current
    val mode = remember {
        if (SDK_INT >= S && ThemeColor.enableMonet(context)) ColorPalette.MODE_MONET_ACCENT_COLOR
        else ColorPalette.MODE_ACCENT_COLOR
    }
    ColorPrefImpl(
        titleRes = R.string.accent_color,
        summaryRes = R.string.accent_color_desc,
        mode = mode
    )
}

@Composable
private fun ColorPrefImpl(
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    mode: Int,
) {
    val context = LocalContext.current

    val color =
        when (mode) {
            ColorPalette.MODE_PRIMARY_COLOR, ColorPalette.MODE_MONET_PRIMARY_COLOR -> MaterialTheme.colors.primary
            ColorPalette.MODE_ACCENT_COLOR, ColorPalette.MODE_MONET_ACCENT_COLOR   -> MaterialTheme.colors.secondary
            else                                                                   -> MaterialTheme.colors.error
        }

    val onClick = {
        when (mode) {
            ColorPalette.MODE_PRIMARY_COLOR, ColorPalette.MODE_ACCENT_COLOR ->
                ColorChooser.showColorChooserDialog(context, color.toArgb(), mode)

            ColorPalette.MODE_MONET_PRIMARY_COLOR                           ->
                MonetColorPickerDialog.primaryColor().show((context as FragmentActivity).supportFragmentManager, null)

            ColorPalette.MODE_MONET_ACCENT_COLOR                            ->
                MonetColorPickerDialog.accentColor().show((context as FragmentActivity).supportFragmentManager, null)
        }
    }

    SettingsMenuLink(
        title = title(titleRes),
        subtitle = subtitle(summaryRes),
        action = { ColorCircle(color = color, modifier = Modifier.fillMaxSize(0.55f), onClick = onClick) },
        onClick = onClick
    )
}

@Composable
private fun MonetSetting() {
    class MonetSettingValueState(val context: Context) : SettingValueState<Boolean> {
        private val _state = mutableStateOf(ThemeColor.enableMonet(context))
        override var value: Boolean
            get() = _state.value
            set(value) {
                _state.value = value
                ThemeColor.edit(context) {
                    enableMonet(value)
                }
            }

        override fun reset() {
            value = true
        }

    }

    val context = LocalContext.current

    val booleanState =
        if (LocalInspectionMode.current) {
            rememberBooleanSettingState(false)
        } else {
            MonetSettingValueState(context)
        }


    BooleanPrefImpl(
        titleRes = R.string.pref_title_enable_monet,
        summaryRes = R.string.pref_summary_enable_monet,
        state = booleanState,
        onCheckedChange = { newValue ->
            DynamicShortcutManager(context).updateDynamicShortcuts()
            (context as? Activity)?.recreate()
        }
    )
}

@Composable
private fun ColoredNavigationBarSetting() {
    class ColoredNavigationBarSettingValueState(val context: Context) : SettingValueState<Boolean> {
        private val _state = mutableStateOf(ThemeColor.coloredNavigationBar(context))
        override var value: Boolean
            get() = _state.value
            set(value) {
                _state.value = value
                ThemeColor.edit(context) {
                    coloredNavigationBar(value)
                }
            }

        override fun reset() {
            value = true
        }
    }

    val booleanState =
        if (LocalInspectionMode.current) {
            rememberBooleanSettingState(false)
        } else {
            ColoredNavigationBarSettingValueState(LocalContext.current)
        }

    val context = LocalContext.current

    BooleanPrefImpl(
        titleRes = R.string.pref_title_navigation_bar,
        summaryRes = R.string.pref_summary_colored_navigation_bar,
        state = booleanState,
        onCheckedChange = {
            (context as? Activity)?.recreate()
        }
    )
}

@Composable
private fun EqualizerSetting() {
    val activity = if (!LocalInspectionMode.current) LocalContext.current as? Activity else null

    val hasEqualizer = remember { mutableStateOf(false) }
    if (!LocalInspectionMode.current) {
        LaunchedEffect("hasEqualizer") {
            hasEqualizer.value = activity?.packageManager
                ?.resolveActivity(Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL), 0) != null
        }
    }

    SettingsMenuLink(
        title = title(R.string.equalizer),
        subtitle = {
            if (!hasEqualizer.value) Text(text = stringResource(id = R.string.no_equalizer))
        }
    ) {
        if (activity != null) {
            NavigationUtil.openEqualizer(activity)
        } else {
            warning("Equalizer", "can not open Equalizer Setting")
        }
    }
}

//endregion

//region Common Preferences

@Composable
private fun BooleanPref(
    key: String,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int = 0,
    defaultValue: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {},
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
    BooleanPrefImpl(
        state = booleanState,
        enabled = enabled,
        titleRes = titleRes,
        summaryRes = summaryRes,
        onCheckedChange = onCheckedChange
    )
}

@Composable
private fun BooleanPrefImpl(
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    state: SettingValueState<Boolean>,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsSwitch(
        state = state,
        enabled = enabled,
        title = title(titleRes),
        subtitle = subtitle(summaryRes),
        modifier = Modifier.fillMaxWidth(),
        onCheckedChange = onCheckedChange,
    )
}


@Composable
private fun DialogPref(
    model: DialogPreferenceModel,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val subtitleState = remember { model.subtitleState }
    LaunchedEffect(model) {
        model.updateSubtitle(context, this) // initial value
    }
    val onDismiss = OnDismissListener {
        model.updateSubtitle(context, null) // updated value
    }
    SettingsMenuLink(
        enabled = enabled,
        title = title(model.titleRes),
        subtitle = subtitle(subtitleState),
        onClick = { model.onShowDialog(context, onDismiss) }
    )
}

internal class DialogPreferenceModel(
    val dialog: Class<out DialogFragment>,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int = 0,
    private val currentValueForHint: (suspend (Context) -> String)? = null,
) {

    val subtitleState = mutableStateOf<String?>(null)

    fun updateSubtitle(context: Context, coroutineScope: CoroutineScope?) {
        val scope = coroutineScope ?: (context as LifecycleOwner).lifecycleScope
        scope.launch(Dispatchers.IO) {
            subtitleState.value = subtitle(context)
        }
    }

    @Suppress("IfThenToElvis")
    suspend fun subtitle(context: Context): String? =
        if (currentValueForHint != null) {
            currentValueForHint.invoke(context)
        } else if (summaryRes != 0) {
            context.getString(summaryRes)
        } else {
            null
        }

    fun onShowDialog(context: Context, onDismissListener: OnDismissListener?) =
        showDialog(context, dialog, onDismissListener)
}

@Composable
private fun ListPref(
    options: OptionGroupModel,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int = 0,
    enabled: Boolean = true,
    onChange: suspend (Int, String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val state = options.rememberSettingState()
    val items = options.items(context)
    val onItemSelected = options.onItemSelected(context, onChange)

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
    Box(Modifier.heightIn(64.dp, 96.dp)) {
        SettingsListDropdown(
            state = state,
            enabled = enabled,
            title = title(titleRes),
            subtitle = subtitle(summaryRes),
            items = items,
            onItemSelected = onItemSelected
        )
    }
}

internal class OptionGroupModel(
    val key: String,
    val optionsValue: List<String>,
    val optionsStringRes: List<Int>,
    val defaultValueIndex: Int = 0,
) {

    private fun optionsStrings(context: Context): List<String> = optionsStringRes.map { context.getString(it) }

    @Composable
    fun rememberSettingState(): SettingValueState<Int> {
        val state = rememberIntSettingState(defaultValueIndex)
        val context = LocalContext.current
        if (!LocalInspectionMode.current) {
            LaunchedEffect(state) {
                state.value = read(context)
            }
        }
        return state
    }

    @Composable
    fun items(context: Context) =
        if (LocalInspectionMode.current) {
            optionsValue
        } else {
            optionsStrings(context)
        }

    @Composable
    fun onItemSelected(
        context: Context,
        onChange: suspend (Int, String) -> Unit = { _, _ -> },
    ): (Int, String) -> Unit =
        if (LocalInspectionMode.current) {
            { _, _ -> }
        } else {
            { index, text ->
                CoroutineScope(Dispatchers.IO).launch {
                    save(context, index)
                    onChange(index, text)
                }
            }
        }

    private suspend fun read(context: Context): Int {
        val value = context.dataStore.data.first()[stringPreferencesKey(key)]
        val index = optionsValue.indexOf(value)
        return if (index > -1) {
            index
        } else if (value == null) {
            Log.v("ListPref", "Preference($key)'s value is unset, ignore!")
            defaultValueIndex
        } else {
            warning("ListPref", "Preference($key)'s value is corrupted or invalid: $value \n reset to default!")
            reset(context)
            defaultValueIndex
        }
    }

    suspend fun save(context: Context, index: Int) {
        context.dataStore.edit { preferences ->
            val newValue = optionsValue.getOrElse(index) { optionsValue[defaultValueIndex] }
            preferences[stringPreferencesKey(key)] = newValue
        }
    }

    suspend fun reset(context: Context) = save(context, defaultValueIndex)
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

//endregion

//region Text
private fun header(res: Int) = @Composable {
    Text(
        text = stringResource(id = res),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp)
    )
}

private fun title(res: Int) = @Composable {
    Text(
        text = stringResource(id = res),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun subtitle(res: Int): (@Composable () -> Unit)? =
    if (res != 0) {
        @Composable {
            Text(
                text = stringResource(id = res),
                modifier = Modifier
            )
        }
    } else {
        null
    }

private fun subtitle(text: MutableState<String?>): (@Composable () -> Unit)? =
    if (text.value != null) {
        @Composable {
            Text(
                text = text.value ?: "",
                modifier = Modifier
            )
        }
    } else {
        null
    }
//endregion


private fun showDialog(
    context: Context,
    dialogClazz: Class<out DialogFragment>,
    onDismissListener: OnDismissListener?,
) {
    @Suppress("LocalVariableName") val TAG = "showDialog"
    val fragmentActivity = context as? FragmentActivity
    if (fragmentActivity != null) {
        try {
            val fragmentManager = fragmentActivity.supportFragmentManager
            val dialog = dialogClazz.getConstructor().newInstance()
            dialog.show(fragmentManager, dialogClazz.simpleName)

            if (onDismissListener != null) {
                dialog.lifecycle.addObserver(
                    object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            super.onDestroy(owner)
                            onDismissListener.onDismiss(dialog.dialog)
                        }
                    }
                )
            }

        } catch (e: Exception) {
            reportError(e, TAG, "Failed to show dialog ${dialogClazz.name}")
        }
    } else {
        warning(TAG, "$context can not show dialog")
    }
}