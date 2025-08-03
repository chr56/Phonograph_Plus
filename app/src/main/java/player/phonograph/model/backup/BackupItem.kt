/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.backup

import player.phonograph.model.backup.BackupType.DATABASE
import player.phonograph.model.backup.BackupType.JSON
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
@Serializable(BackupItem.Serializer::class)
enum class BackupItem(
    val serializationName: String,
    val type: BackupType,
    val deprecated: Boolean = false,
    val enabledByDefault: Boolean = false,
) {
    // Portable
    Settings("setting", JSON, enabledByDefault = true),
    Favorites("favorite", JSON, enabledByDefault = true),
    PlayingQueues("playing_queues", JSON, enabledByDefault = true),
    InternalPlaylists("internal_playlists", JSON),
    // Database
    FavoriteDatabase("database_favorite", DATABASE),
    HistoryDatabase("database_history", DATABASE),
    SongPlayCountDatabase("database_song_play_count", DATABASE),
    PlayingQueuesDatabase("database_music_playback_state", DATABASE),
    MainDatabase("database_main", DATABASE),
    // Deprecated (support import only)
    PathFilter("path_filter", JSON, deprecated = true),
    PathFilterDatabase("database_path_filter", DATABASE, deprecated = true),
    ;

    class Serializer : KSerializer<BackupItem> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("BackupItem", PrimitiveKind.STRING)

        override fun serialize(
            encoder: Encoder,
            value: BackupItem,
        ) = encoder.encodeString(value.serializationName)

        override fun deserialize(decoder: Decoder): BackupItem {
            val serializationName = decoder.decodeString()
            return BackupItem.entries.find { serializationName == it.serializationName }
                ?: throw SerializationException("Unknown key ($serializationName)")
        }
    }

    companion object {
        const val PREFIX_DATABASE = "database_"
    }
}

