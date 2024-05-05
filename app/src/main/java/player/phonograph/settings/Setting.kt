/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceManager
import android.content.Context


class Setting(val context: Context) {

    operator fun <T> get(key: PrimitiveKey<T>): PrimitivePreference<T> =
        PrimitivePreference(key, context)

    @Suppress("PropertyName")
    val Composites = object : CompositesSetting {
        override operator fun <T> get(key: CompositeKey<T>): CompositePreference<T> =
            CompositePreference(key, context)
    }

    interface CompositesSetting {
        operator fun <T> get(key: CompositeKey<T>): CompositePreference<T>
    }

    companion object {
        fun settingsDatastore(context:Context): DataStore<Preferences> = context.settingsDatastore
    }

}

private const val PREFERENCE_NAME = "setting_main"
private val Context.settingsDatastore: DataStore<Preferences>
        by preferencesDataStore(
            name = PREFERENCE_NAME,
            produceMigrations = {
                listOf(
                    SharedPreferencesMigration({ PreferenceManager.getDefaultSharedPreferences(it) })
                )
            }
        )