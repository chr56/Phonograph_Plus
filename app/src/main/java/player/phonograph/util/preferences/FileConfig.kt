/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.preferences

import player.phonograph.App
import player.phonograph.settings.Setting
import player.phonograph.util.FileUtil
import player.phonograph.util.FileUtil.defaultStartDirectory
import androidx.preference.PreferenceManager
import android.content.Context
import java.io.File

object FileConfig {
    var startDirectory: File
        get() = File(startDirectoryPath)
        set(value) {
            startDirectoryPath = FileUtil.safeGetCanonicalPath(value)
        }

    private var startDirectoryPath: String
        get() =
            pref.getString(START_DIRECTORY, defaultStartDirectory.path)
                ?: defaultStartDirectory.path
        set(value) {
            pref.edit().putString(START_DIRECTORY, value).apply()
        }

    private var pref = PreferenceManager.getDefaultSharedPreferences(App.instance)

    private const val START_DIRECTORY = "start_directory"
}