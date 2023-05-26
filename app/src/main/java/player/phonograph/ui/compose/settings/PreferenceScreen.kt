/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.ui.compose.settings

import com.alorma.compose.settings.storage.base.rememberBooleanSettingState
import com.alorma.compose.settings.storage.datastore.rememberPreferenceDataStoreBooleanSettingState
import com.alorma.compose.settings.ui.SettingsSwitch
import player.phonograph.settings.dataStore
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
