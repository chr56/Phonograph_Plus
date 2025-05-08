/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.backup

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class ExportedPathFilter(
    @SerialName("version") val version: Int,
    @SerialName(WHITE_LIST) val whitelist: List<String>,
    @SerialName(BLACK_LIST) val blacklist: List<String>,
) {
    companion object {
        const val VERSION = 0
    }
}

private const val WHITE_LIST = "whitelist"
private const val BLACK_LIST = "blacklist"
