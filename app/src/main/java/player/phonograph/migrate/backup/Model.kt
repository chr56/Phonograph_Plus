/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.migrate.backup

import okio.Buffer
import okio.BufferedSink
import player.phonograph.migrate.DatabaseBackupManger
import player.phonograph.migrate.DatabaseDataManger
import player.phonograph.migrate.SettingDataManager
import player.phonograph.provider.DatabaseConstants
import android.content.Context
import android.content.res.Resources
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.InputStream

@Serializable
class ManifestFile(
    @SerialName(KEY_BACKUP_TIME)
    val timestamp: Long,
    @SerialName(KEY_FILES)
    val files: Map<BackupItem, String>,
    @SerialName(KEY_VERSION)
    val version: Int = VERSION,
) {
    companion object {
        const val BACKUP_MANIFEST_FILENAME = "MANIFEST.json"

        private const val KEY_BACKUP_TIME = "BackupTime"
        private const val KEY_FILES = "files"
        private const val KEY_VERSION = "version"

        private const val VERSION = 1
    }
}

@Serializable(with = BackupItem.Serializer::class)
sealed class BackupItem(
    val key: String,
    val type: Type,
) {
    abstract fun data(context: Context): InputStream

    abstract fun import(inputStream: InputStream, context: Context): Boolean

    open fun displayName(resources: Resources): CharSequence = key

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
            return fromKey(rawString) ?: throw kotlinx.serialization.SerializationException("Unknown key ($rawString)")
        }

    }

    companion object {
        fun fromKey(key: String) = fromKeyImpl(key)
    }
}

private const val KEY_SETTING = "setting"
private const val KEY_PATH_FILTER = "path_filter"
private const val KEY_FAVORITES = "favorite"
private const val KEY_PLAYING_QUEUES = "playing_queues"
private const val KEY_DATABASE_FAVORITE = "database_favorite"
private const val KEY_DATABASE_PATH_FILTER = "database_path_filter"
private const val KEY_DATABASE_HISTORY = "database_history"
private const val KEY_DATABASE_SONG_PLAY_COUNT = "database_song_play_count"
private const val KEY_DATABASE_MUSIC_PLAYBACK_STATE = "database_music_playback_state"

private fun fromSink(block: (BufferedSink) -> Boolean): InputStream {
    val buffer = Buffer()
    block(buffer)
    return buffer.inputStream()
}


object SettingBackup : BackupItem(KEY_SETTING, Type.JSON) {
    override fun data(context: Context): InputStream =
        fromSink {
            SettingDataManager.exportSettings(it)
        }

    override fun import(inputStream: InputStream, context: Context): Boolean =
        SettingDataManager.importSetting(inputStream, context)
}

object PathFilterBackup : BackupItem(KEY_PATH_FILTER, Type.JSON) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseBackupManger.exportPathFilter(it, context)
        }

    override fun import(inputStream: InputStream, context: Context): Boolean =
        DatabaseBackupManger.importPathFilter(context, inputStream)
}

object FavoriteBackup : BackupItem(KEY_FAVORITES, Type.JSON) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseBackupManger.exportFavorites(it, context)
        }

    override fun import(inputStream: InputStream, context: Context): Boolean =
        DatabaseBackupManger.importFavorites(context, inputStream)
}

object PlayingQueuesBackup : BackupItem(KEY_PLAYING_QUEUES, Type.JSON) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseBackupManger.exportPlayingQueues(it, context)
        }

    override fun import(inputStream: InputStream, context: Context): Boolean =
        DatabaseBackupManger.importPlayingQueues(context, inputStream)
}


object FavoriteDatabaseBackup : BackupItem(KEY_DATABASE_FAVORITE, Type.DATABASE) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseDataManger.exportDatabases(it, DatabaseConstants.FAVORITE_DB, context)
        }

    override fun import(inputStream: InputStream, context: Context): Boolean =
        DatabaseDataManger.importSingleDatabases(inputStream, DatabaseConstants.FAVORITE_DB, context)
}

object PathFilterDatabaseBackup : BackupItem(KEY_DATABASE_PATH_FILTER, Type.DATABASE) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseDataManger.exportDatabases(it, DatabaseConstants.PATH_FILTER, context)
        }

    override fun import(inputStream: InputStream, context: Context): Boolean =
        DatabaseDataManger.importSingleDatabases(inputStream, DatabaseConstants.PATH_FILTER, context)
}

object HistoryDatabaseBackup : BackupItem(KEY_DATABASE_HISTORY, Type.DATABASE) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseDataManger.exportDatabases(it, DatabaseConstants.HISTORY_DB, context)
        }

    override fun import(inputStream: InputStream, context: Context): Boolean =
        DatabaseDataManger.importSingleDatabases(inputStream, DatabaseConstants.HISTORY_DB, context)
}

object SongPlayCountDatabaseBackup : BackupItem(KEY_DATABASE_SONG_PLAY_COUNT, Type.DATABASE) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseDataManger.exportDatabases(it, DatabaseConstants.SONG_PLAY_COUNT_DB, context)
        }

    override fun import(inputStream: InputStream, context: Context): Boolean =
        DatabaseDataManger.importSingleDatabases(inputStream, DatabaseConstants.SONG_PLAY_COUNT_DB, context)
}

object MusicPlaybackStateDatabaseBackup : BackupItem(KEY_DATABASE_MUSIC_PLAYBACK_STATE, Type.DATABASE) {
    override fun data(context: Context): InputStream =
        fromSink {
            DatabaseDataManger.exportDatabases(it, DatabaseConstants.MUSIC_PLAYBACK_STATE_DB, context)
        }

    override fun import(inputStream: InputStream, context: Context): Boolean =
        DatabaseDataManger.importSingleDatabases(inputStream, DatabaseConstants.MUSIC_PLAYBACK_STATE_DB, context)
}


// todo
val ALL_BACKUP_CONFIG =
    listOf(
        SettingBackup, FavoriteBackup, PathFilterBackup, PlayingQueuesBackup,
        FavoriteDatabaseBackup,
        PathFilterDatabaseBackup,
        HistoryDatabaseBackup,
        SongPlayCountDatabaseBackup,
        MusicPlaybackStateDatabaseBackup,
    )

private fun fromKeyImpl(key: String): BackupItem? = when (key) {
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
