/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.backup

import androidx.annotation.Keep
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Keep
@Serializable(with = BackupItem.Serializer::class)
sealed class BackupItem(
    val key: String,
    val type: Type,
) {

    /**
     * Type of Backup
     */
    enum class Type(val suffix: String) {
        BINARY("bin"),
        JSON("json"),
        DATABASE("db");
    }

    class Serializer : KSerializer<BackupItem> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("BackupItem", PrimitiveKind.STRING)


        override fun serialize(encoder: Encoder, value: BackupItem) {
            encoder.encodeString(value.key)
        }

        override fun deserialize(decoder: Decoder): BackupItem {
            val rawString = decoder.decodeString()
            return fromKey(rawString) ?: throw SerializationException("Unknown key ($rawString)")
        }
    }

    companion object {
        const val KEY_SETTING = "setting"
        const val KEY_PATH_FILTER = "path_filter"
        const val KEY_FAVORITES = "favorite"
        const val KEY_PLAYING_QUEUES = "playing_queues"
        const val KEY_DATABASE_FAVORITE = "database_favorite"
        const val KEY_DATABASE_PATH_FILTER = "database_path_filter"
        const val KEY_DATABASE_HISTORY = "database_history"
        const val KEY_DATABASE_SONG_PLAY_COUNT = "database_song_play_count"
        const val KEY_DATABASE_MUSIC_PLAYBACK_STATE = "database_music_playback_state"
        const val PREFIX_DATABASE = "database_"

        private fun fromKey(key: String): BackupItem? = when (key) {
            KEY_SETTING                       -> SettingBackup
            KEY_PATH_FILTER                   -> PathFilterBackup
            KEY_FAVORITES                     -> FavoriteBackup
            KEY_PLAYING_QUEUES                -> PlayingQueuesBackup
            KEY_DATABASE_FAVORITE             -> FavoriteDatabaseBackup
            KEY_DATABASE_PATH_FILTER          -> PathFilterDatabaseBackup
            KEY_DATABASE_HISTORY              -> HistoryDatabaseBackup
            KEY_DATABASE_SONG_PLAY_COUNT      -> SongPlayCountDatabaseBackup
            KEY_DATABASE_MUSIC_PLAYBACK_STATE -> MusicPlaybackStateDatabaseBackup
            else                              -> null
        }
    }

    object SettingBackup : BackupItem(KEY_SETTING, Type.JSON)
    object PathFilterBackup : BackupItem(KEY_PATH_FILTER, Type.JSON)
    object FavoriteBackup : BackupItem(KEY_FAVORITES, Type.JSON)
    object PlayingQueuesBackup : BackupItem(KEY_PLAYING_QUEUES, Type.JSON)

    object FavoriteDatabaseBackup : BackupItem(KEY_DATABASE_FAVORITE, Type.DATABASE)
    object PathFilterDatabaseBackup : BackupItem(KEY_DATABASE_PATH_FILTER, Type.DATABASE)
    object HistoryDatabaseBackup : BackupItem(KEY_DATABASE_HISTORY, Type.DATABASE)
    object SongPlayCountDatabaseBackup : BackupItem(KEY_DATABASE_SONG_PLAY_COUNT, Type.DATABASE)
    object MusicPlaybackStateDatabaseBackup : BackupItem(KEY_DATABASE_MUSIC_PLAYBACK_STATE, Type.DATABASE)
}

