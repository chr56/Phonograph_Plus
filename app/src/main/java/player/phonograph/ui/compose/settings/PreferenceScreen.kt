/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.ui.compose.settings

import com.alorma.compose.settings.storage.base.SettingValueState
import com.alorma.compose.settings.storage.base.rememberBooleanSettingState
import com.alorma.compose.settings.storage.base.rememberIntSettingState
import com.alorma.compose.settings.storage.datastore.rememberPreferenceDataStoreBooleanSettingState
import com.alorma.compose.settings.ui.SettingsListDropdown
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import player.phonograph.settings.dataStore
import player.phonograph.util.reportError
import player.phonograph.util.warning
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking



@Composable
private fun BooleanPref(
    key: String,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int = 0,
    enabled: Boolean = true,
) {
    val booleanState =
        if (LocalInspectionMode.current) {
            rememberBooleanSettingState(true)
        } else {
            rememberPreferenceDataStoreBooleanSettingState(key = key, dataStore = LocalContext.current.dataStore)
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
    val state =
        if (LocalInspectionMode.current) {
            rememberIntSettingState(0)
        } else {
            rememberIntSettingState(optionGroup.selected(context))
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
                optionGroup.onSelect(context, index)
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

    fun selected(context: Context): Int = runBlocking {//todo no runBlocking
        val value = context.dataStore.data.first()[stringPreferencesKey(key)]
        optionsValue.indexOf(value)
    }

    fun onSelect(context: Context, index: Int) = runBlocking {
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
    Text(text = stringResource(id = res))
}

private fun subtitle(res: Int): (@Composable () -> Unit)? =
    if (res != 0) {
        @Composable {
            Text(text = stringResource(id = res))
        }
    } else {
        null
    }
//endregion
