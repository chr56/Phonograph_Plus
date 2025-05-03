/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.backup

import okio.Buffer
import okio.BufferedSink
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.mechanism.SettingDataManager
import player.phonograph.model.backup.BackupItem
import player.phonograph.model.backup.BackupItem.FavoriteBackup
import player.phonograph.model.backup.BackupItem.FavoriteDatabaseBackup
import player.phonograph.model.backup.BackupItem.HistoryDatabaseBackup
import player.phonograph.model.backup.BackupItem.MusicPlaybackStateDatabaseBackup
import player.phonograph.model.backup.BackupItem.PathFilterBackup
import player.phonograph.model.backup.BackupItem.PathFilterDatabaseBackup
import player.phonograph.model.backup.BackupItem.PlayingQueuesBackup
import player.phonograph.model.backup.BackupItem.SettingBackup
import player.phonograph.model.backup.BackupItem.SongPlayCountDatabaseBackup
import player.phonograph.model.backup.BackupManifestFile
import player.phonograph.repo.database.DatabaseConstants
import player.phonograph.util.file.createOrOverrideFile
import player.phonograph.util.text.currentTimestamp
import player.phonograph.util.transferToOutputStream
import player.phonograph.util.warning
import player.phonograph.util.zip.ZipUtil.addToZipFile
import player.phonograph.util.zip.ZipUtil.extractZipFile
import android.content.Context
import android.content.res.Resources
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object Backup {

    val ALL_BACKUP_CONFIG =
        listOf(
            SettingBackup, FavoriteBackup, PathFilterBackup, PlayingQueuesBackup,
            FavoriteDatabaseBackup,
            PathFilterDatabaseBackup,
            HistoryDatabaseBackup,
            SongPlayCountDatabaseBackup,
            MusicPlaybackStateDatabaseBackup,
        )

    val ENABLE_BACKUP_CONFIG = listOf(
        SettingBackup, FavoriteBackup, PathFilterBackup, PlayingQueuesBackup,
    )

    fun displayName(backupItem: BackupItem, resources: Resources): CharSequence = with(resources) {
        when (backupItem) {
            SettingBackup                    -> getString(R.string.action_settings)
            PathFilterBackup                 -> getString(R.string.path_filter)
            FavoriteBackup                   -> getString(R.string.favorites)
            PlayingQueuesBackup              -> getString(R.string.label_playing_queue)
            FavoriteDatabaseBackup           -> "[${getString(R.string.databases)}] ${getString(R.string.favorites)}"
            PathFilterDatabaseBackup         -> "[${getString(R.string.databases)}] ${getString(R.string.path_filter)}"
            HistoryDatabaseBackup            -> "[${getString(R.string.databases)}] ${getString(R.string.history)}"
            SongPlayCountDatabaseBackup      -> "[${getString(R.string.databases)}] ${getString(R.string.my_top_tracks)}"
            MusicPlaybackStateDatabaseBackup -> "[${getString(R.string.databases)}] ${getString(R.string.label_playing_queue)}"
        }
    }

    object Export {

        suspend fun exportBackupToArchive(
            context: Context,
            config: List<BackupItem>,
            targetOutputStream: OutputStream,
        ) {

            val (session, tmpDir) = SessionManger.newSession(context)

            exportBackupToDirectory(context, config, tmpDir)

            ZipOutputStream(targetOutputStream).use { zip ->
                tmpDir.listFiles()?.forEach {
                    addToZipFile(zip, it, it.name)
                }
            }

            SessionManger.terminateSession(session)
        }


        private suspend fun exportBackupToDirectory(
            context: Context,
            config: List<BackupItem>,
            destination: File,
        ) {
            val timestamp = currentTimestamp()

            val fileList = mutableMapOf<BackupItem, String>() // track files added

            // export backups
            for (item in config) {
                val filename = "${item.key}.${item.type.suffix}"
                val exported = read(context, item)
                if (exported != null) {
                    val file = File(destination, filename).createOrOverrideFile()
                    file.outputStream().use { outputStream ->
                        exported.use { inputStream ->
                            inputStream.transferToOutputStream(outputStream)
                        }
                    }
                    fileList[item] = filename
                } else {
                    warning(TAG, "No content to export for ${item.key}")
                }
            }

            // generate manifest file
            val manifestFile = File(destination, BackupManifestFile.BACKUP_MANIFEST_FILENAME).createOrOverrideFile()
            manifestFile.outputStream().bufferedWriter().use {
                val manifest =
                    BackupManifestFile(timestamp, fileList, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
                val raw = parser.encodeToString(manifest)
                it.write(raw)
                it.flush()
            }
        }

        private suspend fun fromSink(block: suspend (BufferedSink) -> Boolean): InputStream? {
            val buffer = Buffer()
            val result = block(buffer)
            return if (result) buffer.inputStream() else null
        }

        private suspend fun read(context: Context, item: BackupItem): InputStream? = when (item) {
            SettingBackup                    -> fromSink { SettingDataManager.exportSettings(it, context) }
            PathFilterBackup                 -> fromSink { DatabaseDataManger.exportPathFilter(it, context) }
            FavoriteBackup                   -> fromSink { DatabaseDataManger.exportFavorites(it, context) }
            PlayingQueuesBackup              -> fromSink { DatabaseDataManger.exportPlayingQueues(it, context) }
            FavoriteDatabaseBackup           -> fromSink {
                DatabaseManger.exportDatabase(it, DatabaseConstants.FAVORITE_DB, context)
            }

            PathFilterDatabaseBackup         -> fromSink {
                DatabaseManger.exportDatabase(it, DatabaseConstants.PATH_FILTER, context)
            }

            HistoryDatabaseBackup            -> fromSink {
                DatabaseManger.exportDatabase(it, DatabaseConstants.HISTORY_DB, context)
            }

            SongPlayCountDatabaseBackup      -> fromSink {
                DatabaseManger.exportDatabase(it, DatabaseConstants.SONG_PLAY_COUNT_DB, context)
            }

            MusicPlaybackStateDatabaseBackup -> fromSink {
                DatabaseManger.exportDatabase(it, DatabaseConstants.MUSIC_PLAYBACK_STATE_DB, context)
            }
        }

    }

    object Import {

        /**
         * @return session ID
         */
        fun startImportBackupFromArchive(
            context: Context,
            sourceInputStream: InputStream,
        ): Long {
            val (session, tmpDir) = SessionManger.newSession(context)

            ZipInputStream(sourceInputStream).use { zipIn ->
                extractZipFile(zipIn, tmpDir)
            }

            return session
        }

        fun readManifest(session: Long): BackupManifestFile? {
            val tmpDir = SessionManger.sessionDirectory(session)
            val manifestFile = File(tmpDir, BackupManifestFile.BACKUP_MANIFEST_FILENAME)
            return when {
                manifestFile.exists() -> decodeManifest(manifestFile)
                tmpDir != null        -> guessManifest(tmpDir)
                else                  -> null
            }
        }

        fun executeImport(
            context: Context,
            session: Long,
            content: Iterable<BackupItem>,
            onUpdateProgress: (CharSequence) -> Unit,
        ) {
            val tmpDir = SessionManger.sessionDirectory(session)
            val manifest = readManifest(session)
            require(manifest != null) { "No Manifest!" }
            // filter
            val selected = manifest.files.filterKeys { it in content }
            for ((item, relativePath) in selected) {
                onUpdateProgress(displayName(item, context.resources))
                FileInputStream(File(tmpDir, relativePath)).use { inputStream ->
                    import(context, inputStream, item)
                }
            }
        }

        private fun import(context: Context, inputStream: InputStream, item: BackupItem): Boolean {
            return when (item) {
                SettingBackup                    -> SettingDataManager.importSetting(inputStream, context)
                PathFilterBackup                 -> DatabaseDataManger.importPathFilter(context, inputStream)
                FavoriteBackup                   -> DatabaseDataManger.importFavorites(context, inputStream)
                PlayingQueuesBackup              -> DatabaseDataManger.importPlayingQueues(context, inputStream)

                FavoriteDatabaseBackup           ->
                    DatabaseManger.importDatabase(inputStream, DatabaseConstants.FAVORITE_DB, context)

                PathFilterDatabaseBackup         ->
                    DatabaseManger.importDatabase(inputStream, DatabaseConstants.PATH_FILTER, context)

                HistoryDatabaseBackup            ->
                    DatabaseManger.importDatabase(inputStream, DatabaseConstants.HISTORY_DB, context)

                SongPlayCountDatabaseBackup      ->
                    DatabaseManger.importDatabase(inputStream, DatabaseConstants.SONG_PLAY_COUNT_DB, context)

                MusicPlaybackStateDatabaseBackup ->
                    DatabaseManger.importDatabase(inputStream, DatabaseConstants.MUSIC_PLAYBACK_STATE_DB, context)
            }
        }

        fun endImportBackupFromArchive(
            session: Long,
        ) {
            SessionManger.terminateSession(session)
        }

        private fun decodeManifest(inputFile: File): BackupManifestFile {
            val manifestFile = inputFile.inputStream().bufferedReader().use {
                val raw = it.readText()
                parser.decodeFromString<BackupManifestFile>(raw)
            }
            return manifestFile
        }

        private fun guessManifest(dir: File): BackupManifestFile? {
            require(dir.isDirectory)
            val files = dir.list()
            if (files != null && files.isNotEmpty()) {
                val map = mutableMapOf<BackupItem, String>()
                for (fileName in files) {
                    for (item in ALL_BACKUP_CONFIG) {
                        when {
                            fileName.endsWith(BackupItem.Type.DATABASE.suffix) -> { // special for database
                                if (fileName.endsWith(item.type.suffix, true) &&
                                    fileName.contains(item.key.removePrefix(BackupItem.PREFIX_DATABASE), true)
                                ) {
                                    map[item] = fileName
                                }
                            }

                            else                                               -> {
                                if (fileName.endsWith(item.type.suffix, true) &&
                                    fileName.contains(item.key, true)
                                ) {
                                    map[item] = fileName
                                }
                            }
                        }
                    }

                }
                return BackupManifestFile(dir.lastModified(), map, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
            }
            warning(TAG, "Couldn't analysis the content of this backup")
            return null
        }


    }

    private object SessionManger {
        private val sessions = mutableMapOf<Long, File>()

        fun sessionDirectory(sessionId: Long) = sessions[sessionId]

        fun newSession(context: Context): Pair<Long, File> {
            val sessionId = currentTimestamp()
            val cacheDir = context.externalCacheDir ?: context.cacheDir!!
            val tmpFile = File(cacheDir, "${TMP_BACKUP_FOLDER_PREFIX}_$sessionId").also { it.mkdirs() }
            sessions[sessionId] = tmpFile
            return sessionId to tmpFile
        }

        fun terminateSession(sessionId: Long) {
            val file = sessions.remove(sessionId)
            file?.deleteRecursively()
        }

        private const val TMP_BACKUP_FOLDER_PREFIX = "BackupTmp"
    }

    private val parser by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            encodeDefaults = true
        }
    }

    private const val TAG = "Backup"
}