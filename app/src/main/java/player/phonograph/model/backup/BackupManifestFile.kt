/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.backup

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
class BackupManifestFile(
    @SerialName(KEY_BACKUP_TIME)
    val timestamp: Long,
    @SerialName(KEY_FILES)
    val files: Map<BackupItem, String>,
    @SerialName(KEY_PHONOGRAPH_VERSION)
    val phonographVersion: String,
    @SerialName(KEY_PHONOGRAPH_VERSION_CODE)
    val phonographVersionCode: Int,
    @SerialName(KEY_VERSION)
    val version: Int = VERSION,
) {

    companion object {
        const val BACKUP_MANIFEST_FILENAME = "MANIFEST.json"

        private const val KEY_BACKUP_TIME = "BackupTime"
        private const val KEY_FILES = "files"
        private const val KEY_VERSION = "version"
        private const val KEY_PHONOGRAPH_VERSION = "phonograph_version"
        private const val KEY_PHONOGRAPH_VERSION_CODE = "phonograph_version_code"

        private const val VERSION = 1
    }
}