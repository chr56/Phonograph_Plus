/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting.components

import player.phonograph.settings.CompositeKey
import player.phonograph.settings.Preference
import player.phonograph.settings.PreferenceKey
import player.phonograph.settings.PrimitiveKey
import player.phonograph.settings.Setting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


@Composable
fun <T> rememberSettingPreference(key: PreferenceKey<T>): Preference<T> {
    return if (LocalInspectionMode.current) {
        rememberInMemorySettingPreference(key)
    } else {
        rememberDataStoreSettingPreference(key)
    }
}

@Composable
fun <T> rememberInMemorySettingPreference(key: PreferenceKey<T>): Preference<T> {
    return remember { DummyPreference(key) }
}

@Composable
fun <T> rememberDataStoreSettingPreference(key: PreferenceKey<T>): Preference<T> {
    val context = LocalContext.current
    return remember { Setting(context)[key] }
}

/**
 * Preference in memory for testing
 */
private class DummyPreference<T>(key: PreferenceKey<T>) : Preference<T> {

    private val _default = when (key) {
        is PrimitiveKey -> key.defaultValue
        is CompositeKey -> key.valueProvider.defaultValue
    }

    private val _flow: MutableStateFlow<T> = MutableStateFlow(_default())

    override val default: T = _default()

    override val flow: Flow<T> get() = _flow.asStateFlow()

    override suspend fun read(): T = _flow.value

    override suspend fun edit(value: () -> T) {
        _flow.value = value()
    }

}
