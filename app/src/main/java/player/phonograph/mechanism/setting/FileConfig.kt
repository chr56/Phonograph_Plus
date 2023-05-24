/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.setting

import player.phonograph.App
import player.phonograph.settings.dataStore
import player.phonograph.util.FileUtil
import player.phonograph.util.FileUtil.defaultStartDirectory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

object FileConfig {
    var startDirectory: File
        get() = File(startDirectoryPath)
        set(value) {
            startDirectoryPath = FileUtil.safeGetCanonicalPath(value)
        }

    private var startDirectoryPath: String
        get() =
            runBlocking {
                App.instance.dataStore.data.first()[stringPreferencesKey(START_DIRECTORY)] ?: defaultStartDirectory.path
            }
        set(value) {
            runBlocking {
                App.instance.dataStore.edit {
                    it[stringPreferencesKey(START_DIRECTORY)] = value
                }
            }
        }

    private const val START_DIRECTORY = "start_directory"
}