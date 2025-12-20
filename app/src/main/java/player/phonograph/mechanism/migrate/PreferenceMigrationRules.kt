/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.migrate

import player.phonograph.foundation.error.warning
import player.phonograph.model.migration.VersionMigrationRule
import player.phonograph.settings.Setting
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

//region Migration Rules

class PlaylistFilesOperationBehaviourMigrationRule : PreferenceMigrationRule(introduced = 1085) {
    override fun execute(context: Context) {
        removeSettingItem(
            context,
            DeprecatedPreference.PLAYLIST_FILES_OPERATION_BEHAVIOUR
        )
    }
}

class ColoredSystemBarsMigrationRule : PreferenceMigrationRule(introduced = 1086) {
    override fun execute(context: Context) {
        removeSettingItem(context, DeprecatedPreference.COLORED_NAVIGATION_BAR)
        removeSettingItem(context, DeprecatedPreference.COLORED_STATUSBAR)
    }
}

class PreloadImagesMigrationRule : PreferenceMigrationRule(introduced = 1100) {
    override fun execute(context: Context) {
        removeSettingItem(context, DeprecatedPreference.PRELOAD_IMAGES)
    }
}

class NowPlayingScreenMigrationRule : PreferenceMigrationRule(introduced = 1100) {
    override fun execute(context: Context) {
        removeSettingItem(context, DeprecatedPreference.NOW_PLAYING_SCREEN_ID)
    }
}
//endregion

sealed class PreferenceMigrationRule(introduced: Int) : VersionMigrationRule(introduced) {

    protected fun removeSettingItem(context: Context, key: String) {
        try {
            CoroutineScope(SupervisorJob()).launch {
                Setting(context).dataStore.edit { preference ->
                    val booleanKey = booleanPreferencesKey(key)
                    val stringKey = stringPreferencesKey(key)
                    val stringSetKey = stringSetPreferencesKey(key)
                    val intKey = intPreferencesKey(key)
                    val longKey = longPreferencesKey(key)
                    val floatKey = floatPreferencesKey(key)
                    val keys: List<Preferences.Key<*>> = listOf(
                        booleanKey, stringKey, stringSetKey, intKey, longKey, floatKey
                    )
                    for (key in keys) {
                        if (preference.contains(key)) {
                            preference.remove(key)
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            warning(context, TAG, "Failed to remove legacy setting item `$key` via datastore", e)
        }
    }

    protected fun deleteSharedPreferences(context: Context, fileName: String) {
        val sharedPreferencesFile = File("${context.applicationInfo.dataDir}/shared_prefs/$fileName.xml")
        if (sharedPreferencesFile.exists()) {
            sharedPreferencesFile.delete()
        }
    }

    companion object {
        private const val TAG = "PreferenceMigrationRule"
    }
}
