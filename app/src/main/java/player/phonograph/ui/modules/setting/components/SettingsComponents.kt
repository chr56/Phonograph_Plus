/*
 *  Copyright (c) 2022~2025 chr_56
 */


package player.phonograph.ui.modules.setting.components

import player.phonograph.settings.PreferenceKey
import player.phonograph.ui.compose.components.ColorCircle
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.content.Context
import android.content.DialogInterface.OnDismissListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun SettingsGroup(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsGroup(title = header(titleRes), modifier = modifier, content = content)
}

@Composable
fun BooleanPreference(
    key: PreferenceKey<Boolean>,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int = 0,
    enabled: Boolean = true,
    writerCoroutineScope: CoroutineScope = rememberCoroutineScope(),
    onCheckedChange: (Boolean) -> Unit = {},
) {
    val preference = rememberSettingPreference(key)
    val value by preference.flow.collectAsState(preference.default)
    SettingsSwitch(
        value = value,
        enabled = enabled,
        title = title(titleRes),
        subtitle = subtitle(summaryRes),
        onCheckedChange = {
            writerCoroutineScope.launch(Dispatchers.IO) { preference.edit { it } }
            onCheckedChange(it)
        },
    )
}

@Composable
fun BooleanPreference(
    key: PreferenceKey<Boolean>,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int = 0,
    enabled: Boolean = true,
    writerCoroutineScope: CoroutineScope = rememberCoroutineScope(),
    currentValueForHint: suspend (Context, Boolean) -> String?,
) {
    val context = LocalContext.current

    val preference = rememberSettingPreference(key)
    val value by preference.flow.collectAsState(preference.default)

    val defaultSubtitle = if (summaryRes != 0) stringResource(summaryRes) else null
    var subtitle by remember { mutableStateOf<String?>(defaultSubtitle) }

    var version by remember { mutableIntStateOf(0) }
    LaunchedEffect(version) {
        subtitle = currentValueForHint(context, value) ?: defaultSubtitle
    }

    SettingsSwitch(
        value = value,
        enabled = enabled,
        title = title(titleRes),
        subtitle = subtitle(subtitle),
        onCheckedChange = {
            writerCoroutineScope.launch(Dispatchers.IO) {
                preference.edit { it }
                version++
            }
        },
    )
}

@Composable
fun DialogPreference(
    dialog: Class<out DialogFragment>,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int = 0,
    enabled: Boolean = true,
    reset: (suspend (Context) -> Unit)? = null,
    writerCoroutineScope: CoroutineScope = rememberCoroutineScope(),
    currentValueForHint: suspend (Context) -> String? = { null },
) {
    val context = LocalContext.current

    val defaultSubtitle = if (summaryRes != 0) stringResource(summaryRes) else ""
    var subtitle by remember { mutableStateOf<String>(defaultSubtitle) }

    var version by remember { mutableIntStateOf(0) }
    LaunchedEffect(version) {
        subtitle = currentValueForHint.invoke(context) ?: defaultSubtitle
    }

    val onLongClick: (() -> Unit)? =
        if (reset != null) {
            { writerCoroutineScope.launch(Dispatchers.IO) { reset(context) } }
        } else {
            null
        }

    SettingsExternal(
        enabled = enabled,
        title = title(titleRes),
        subtitle = subtitle(subtitle),
        onClick = {
            showDialog(context as FragmentActivity, dialog) { version++ }
        },
        onLongClick = onLongClick
    )
}

@Composable
fun ListPreference(
    key: PreferenceKey<String>,
    optionsValues: List<String>,
    optionsValuesLocalized: List<Int>,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int = 0,
    enabled: Boolean = true,
    writerCoroutineScope: CoroutineScope = rememberCoroutineScope(),
    onChange: (Int, String) -> Unit = { _, _ -> },
) {
    val preference = rememberSettingPreference(key)

    val current by preference.flow.collectAsState(preference.default)
    val selected = optionsValues.indexOf(current).coerceIn(optionsValues.indices)
    val options = optionsValuesLocalized.map { stringResource(it) }
    val onOptionItemSelected: (Int, String) -> Unit = { index, text ->
        writerCoroutineScope.launch(Dispatchers.IO) {
            preference.edit { optionsValues[index] }
        }
        onChange(index, text)
    }

    Box(Modifier.heightIn(64.dp, 96.dp)) {
        SettingsListDropdown(
            selected = selected,
            title = title(titleRes),
            options = options,
            onOptionItemSelected = onOptionItemSelected,
            enabled = enabled,
            subtitle = subtitle(summaryRes),
        )
    }
}

@Composable
fun FloatPreference(
    key: PreferenceKey<Float>,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int = 0,
    enabled: Boolean = true,
    writerCoroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    val preference = rememberSettingPreference(key)
    val value by preference.flow.collectAsState(preference.default)
    var staging by remember(value) { mutableFloatStateOf(value) }

    val defaultSubtitle = if (summaryRes != 0) stringResource(summaryRes) else null
    var subtitle by remember { mutableStateOf<String?>(defaultSubtitle) }

    SettingsSlider(
        value = staging,
        valueRange = valueRange,
        enabled = enabled,
        title = title(titleRes),
        subtitle = subtitle(subtitle),
        colors =
            SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.secondary,
                activeTrackColor = MaterialTheme.colors.secondary,
            ),
        steps = steps,
        onValueChange = { staging = it },
        onValueChangeFinished = {
            writerCoroutineScope.launch {
                preference.edit { staging }
            }
        }
    )

}

@Composable
fun ColorPreference(
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    color: Color,
    onClick: () -> Unit,
) {
    ExternalPreference(
        titleRes = titleRes,
        summaryRes = summaryRes,
        action = { ColorCircle(color = color, modifier = Modifier.fillMaxSize(0.55f), onClick = onClick) },
        onClick = onClick
    )
}


@Composable
fun ExternalPreference(
    titleRes: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    summaryRes: Int = 0,
    action: (@Composable (Boolean) -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    SettingsMenuLink(
        title = title(titleRes),
        subtitle = subtitle(summaryRes),
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        action = action,
        onClick = onClick
    )
}

fun showDialog(
    fragmentActivity: FragmentActivity,
    dialogClazz: Class<out DialogFragment>,
    onDismissListener: OnDismissListener?,
) {
    val fragmentManager = fragmentActivity.supportFragmentManager
    val dialog = try {
        dialogClazz.getConstructor().newInstance()
    } catch (e: Exception) {
        Log.e("SettingsUI", "Failed to show dialog ${dialogClazz.name}", e)
        null
    } ?: return
    dialog.show(fragmentManager, dialogClazz.simpleName)
    if (onDismissListener != null) {
        dialog.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    onDismissListener.onDismiss(dialog.dialog)
                    owner.lifecycle.removeObserver(this)
                }
            }
        )
    }
}


//region Elements builders
private fun header(res: Int): @Composable () -> Unit = @Composable {
    Text(
        text = stringResource(id = res),
        fontWeight = FontWeight.SemiBold,
    )
}

private fun title(res: Int): @Composable () -> Unit = @Composable {
    Text(
        text = stringResource(id = res),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun subtitle(res: Int): (@Composable () -> Unit)? =
    if (res > 0) {
        @Composable { Text(text = stringResource(id = res)) }
    } else {
        null
    }

private fun subtitle(text: String?): (@Composable () -> Unit)? =
    if (text != null) {
        @Composable { Text(text = text) }
    } else {
        null
    }
//endregion