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

private const val PREFERENCE_NAME = "setting_main"
val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(
            name = PREFERENCE_NAME,
            produceMigrations = {
                listOf(
                    SharedPreferencesMigration({ PreferenceManager.getDefaultSharedPreferences(it) })
                )
            }
        )