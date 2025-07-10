/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.backup

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

//region Metadata


/**
 * Type of Backup
 */
enum class BackupType(val suffix: String) {
    BINARY("bin"),
    JSON("json"),
    DATABASE("db");
}

//endregion

//region Elements
@Keep
@Serializable
class ExportedSong(
    @SerialName("path") val path: String,
    @SerialName("title") val title: String,
    @SerialName("album") val album: String?,
    @SerialName("artist") val artist: String?,
)

@Keep
@Serializable
class ExportedPlaylist(
    @SerialName("path") val path: String,
    @SerialName("title") val name: String,
)

@Keep
@Serializable
class ExportedInternalPlaylist(
    @SerialName("name") val name: String,
    @SerialName("songs") val songs: List<ExportedSong>,
    @SerialName("date_added") val dateAdded: Long = 0,
    @SerialName("date_modified") val dateModified: Long = 0,
)
//endregion

//region Items

@Keep
@Serializable
class ExportedInternalPlaylists(
    @SerialName("version") val version: Int,
    @SerialName(PLAYLISTS) val playlists: List<ExportedInternalPlaylist>,
) {
    companion object {
        private const val PLAYLISTS = "playlists"

        const val VERSION = 0
    }
}

@Keep
@Serializable
class ExportedFavorites(
    @SerialName("version") val version: Int,
    @SerialName(FAVORITE_SONG) val favoriteSong: List<ExportedSong>,
    @SerialName(PINED_PLAYLIST) val pinedPlaylist: List<ExportedPlaylist>,
) {
    companion object {
        private const val FAVORITE_SONG = "favorite"
        private const val PINED_PLAYLIST = "pined_playlists"

        const val VERSION = 0
    }
}

@Keep
@Serializable
data class ExportedPlayingQueue(
    @SerialName("version") val version: Int,
    @SerialName(PLAYING_QUEUE) val playingQueue: List<ExportedSong>,
    @SerialName(ORIGINAL_PLAYING_QUEUE) val originalPlayingQueue: List<ExportedSong>,
) {
    companion object {
        private const val PLAYING_QUEUE = "playing_queue"
        private const val ORIGINAL_PLAYING_QUEUE = "original_playing_queue"

        const val VERSION = 0
    }
}


@Keep
@Serializable
data class ExportedPathFilter(
    @SerialName("version") val version: Int,
    @SerialName(WHITE_LIST) val whitelist: List<String>,
    @SerialName(BLACK_LIST) val blacklist: List<String>,
) {
    companion object {
        private const val WHITE_LIST = "whitelist"
        private const val BLACK_LIST = "blacklist"

        const val VERSION = 0
    }
}

@Keep
@Serializable
data class ExportedSetting(
    @SerialName("format_version") val formatVersion: Int,
    @SerialName(APP_VERSION) val appVersion: Int,
    @SerialName(COMMIT_HASH) val commitHash: String,
    @SerialName(CONTENT) val content: JsonObject,
) {
    companion object {
        private const val APP_VERSION = "app_version"
        private const val COMMIT_HASH = "commit_hash"
        private const val CONTENT = "content"

        const val VERSION = 2
    }
}
//endregion
