/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.preferences

import player.phonograph.settings.Setting
import player.phonograph.util.FileUtil
import java.io.File

object FileConfig {
    var startDirectory: File
        get() = File(Setting.instance.startDirectoryPath)
        set(value) {
            Setting.instance.startDirectoryPath = FileUtil.safeGetCanonicalPath(value)
        }
}