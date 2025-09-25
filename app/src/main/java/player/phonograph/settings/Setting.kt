/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context


class Setting(context: Context) {

    val dataStore = context.applicationContext.settingsDatastore

    operator fun <T> get(key: PreferenceKey<T>): Preference<T> =
        when (key) {
            is PrimitiveKey<T> -> PrimitivePreference(key, dataStore)
            is CompositeKey<T> -> CompositePreference(key, dataStore)
        }

    /**
     * This would **CLEAR ALL SETTINGS**!
     */
    suspend fun clearAll(): Boolean = try {
        dataStore.edit { it.clear() }
        true
    } catch (_: Exception) {
        false
    }

    companion object {

        private const val PREFERENCE_NAME = "setting_main"

        @JvmStatic
        private val Context.settingsDatastore: DataStore<Preferences>
                by preferencesDataStore(
                    name = PREFERENCE_NAME,
                )
    }

}
