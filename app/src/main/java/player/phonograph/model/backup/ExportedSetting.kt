/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonObject


@kotlinx.serialization.Serializable
data class ExportedSetting(
    @SerialName(FORMAT_VERSION) val formatVersion: Int,
    @SerialName(APP_VERSION) val appVersion: Int,
    @SerialName(COMMIT_HASH) val commitHash: String,
    @SerialName(CONTENT) val content: JsonObject,
) {
    companion object {
        const val VERSION = 2
    }
}

private const val FORMAT_VERSION = "format_version"
private const val APP_VERSION = "app_version"
private const val COMMIT_HASH = "commit_hash"
private const val CONTENT = "content"