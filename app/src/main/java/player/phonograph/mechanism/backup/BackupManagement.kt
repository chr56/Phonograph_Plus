/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.backup

import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.foundation.error.warning
import player.phonograph.model.backup.BackupItem
import player.phonograph.model.backup.BackupItem.FavoriteBackup
import player.phonograph.model.backup.BackupItem.FavoriteDatabaseBackup
import player.phonograph.model.backup.BackupItem.HistoryDatabaseBackup
import player.phonograph.model.backup.BackupItem.InternalPlaylistsBackup
import player.phonograph.model.backup.BackupItem.MainDatabaseBackup
import player.phonograph.model.backup.BackupItem.MusicPlaybackStateDatabaseBackup
import player.phonograph.model.backup.BackupItem.PathFilterBackup
import player.phonograph.model.backup.BackupItem.PathFilterDatabaseBackup
import player.phonograph.model.backup.BackupItem.PlayingQueuesBackup
import player.phonograph.model.backup.BackupItem.SettingBackup
import player.phonograph.model.backup.BackupItem.SongPlayCountDatabaseBackup
import player.phonograph.model.backup.BackupItemExecutor
import player.phonograph.model.backup.BackupManifestFile
import player.phonograph.repo.database.DatabaseConstants.FAVORITE_DB
import player.phonograph.repo.database.DatabaseConstants.HISTORY_DB
import player.phonograph.repo.database.DatabaseConstants.MUSIC_PLAYBACK_STATE_DB
import player.phonograph.repo.database.DatabaseConstants.PATH_FILTER
import player.phonograph.repo.database.DatabaseConstants.SONG_PLAY_COUNT_DB
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.util.text.currentTimestamp
import player.phonograph.util.zip.ZipUtil.extractDirectory
import player.phonograph.util.zip.ZipUtil.zipDirectory
import android.content.Context
import android.content.res.Resources
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object Backup {

    val ALL_BACKUP_CONFIG =
        listOf(
            SettingBackup, FavoriteBackup, PathFilterBackup, PlayingQueuesBackup,
            InternalPlaylistsBackup,
            MainDatabaseBackup,
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
            FavoriteBackup                   -> getString(R.string.playlist_favorites)
            PlayingQueuesBackup              -> getString(R.string.label_playing_queue)
            InternalPlaylistsBackup          -> getString(R.string.label_database_playlists)
            MainDatabaseBackup               -> "[${getString(R.string.label_databases)}] ${getString(R.string.pref_header_library)}"
            FavoriteDatabaseBackup           -> "[${getString(R.string.label_databases)}] ${getString(R.string.playlist_favorites)}"
            PathFilterDatabaseBackup         -> "[${getString(R.string.label_databases)}] ${getString(R.string.path_filter)}"
            HistoryDatabaseBackup            -> "[${getString(R.string.label_databases)}] ${getString(R.string.playlist_history)}"
            SongPlayCountDatabaseBackup      -> "[${getString(R.string.label_databases)}] ${getString(R.string.playlist_my_top_tracks)}"
            MusicPlaybackStateDatabaseBackup -> "[${getString(R.string.label_databases)}] ${getString(R.string.label_playing_queue)}"
        }
    }

    private fun executor(item: BackupItem): BackupItemExecutor? = when (item) {
        SettingBackup                    -> SettingsDataBackupItemExecutor
        PathFilterBackup                 -> PathFilterDataBackupItemExecutor
        FavoriteBackup                   -> FavoritesDataBackupItemExecutor
        PlayingQueuesBackup              -> PlayingQueuesDataBackupItemExecutor
        InternalPlaylistsBackup          -> InternalDatabasePlaylistsDataBackupItemExecutor
        MainDatabaseBackup               -> RawDatabaseBackupItemExecutor(MusicDatabase.DATABASE_NAME)
        FavoriteDatabaseBackup           -> RawDatabaseBackupItemExecutor(FAVORITE_DB)
        PathFilterDatabaseBackup         -> RawDatabaseBackupItemExecutor(PATH_FILTER)
        HistoryDatabaseBackup            -> RawDatabaseBackupItemExecutor(HISTORY_DB)
        SongPlayCountDatabaseBackup      -> RawDatabaseBackupItemExecutor(SONG_PLAY_COUNT_DB)
        MusicPlaybackStateDatabaseBackup -> RawDatabaseBackupItemExecutor(MUSIC_PLAYBACK_STATE_DB)
    }

    object Export {

        suspend fun exportBackupToArchive(
            context: Context,
            config: List<BackupItem>,
            targetOutputStream: OutputStream,
        ) {
            val (session, tmpDir) = SessionManger.newSession(context)

            exportBackupToDirectory(context, config, tmpDir)

            zipDirectory(targetOutputStream, tmpDir)

            SessionManger.terminateSession(session)
        }


        private suspend fun exportBackupToDirectory(
            context: Context,
            config: List<BackupItem>,
            destination: File,
        ) {
            val timestamp = currentTimestamp()

            val files = mutableMapOf<BackupItem, String>() // track files added

            val destinationPath = destination.toOkioPath()
            val fs = okio.FileSystem.SYSTEM

            // export backups
            for (item in config) {

                val executor = executor(item)
                val exported = if (executor != null) {
                    executor.export(context)
                } else {
                    warning(context, TAG, "$item could not be exported!")
                    null
                }

                if (exported != null) {
                    val filename = "${item.key}.${item.type.suffix}"
                    val path = destinationPath / filename
                    fs.write(path, mustCreate = true) {
                        exported.use { writeAll(it) }
                    }
                    files[item] = filename
                } else {
                    warning(context, TAG, "No content to export for ${item.key}")
                }
            }

            // generate manifest file
            val manifestPath = destinationPath / BackupManifestFile.BACKUP_MANIFEST_FILENAME
            fs.write(manifestPath, mustCreate = true) {
                val manifest = BackupManifestFile(timestamp, files, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
                val content = parser.encodeToString(manifest)
                writeUtf8(content)
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

            extractDirectory(sourceInputStream, tmpDir)

            return session
        }

        fun readManifest(context: Context, session: Long): BackupManifestFile? {
            val tmpDir = SessionManger.sessionDirectory(session)
            val manifestFile = File(tmpDir, BackupManifestFile.BACKUP_MANIFEST_FILENAME)
            return when {
                manifestFile.exists() -> decodeManifest(manifestFile)
                tmpDir != null        -> guessManifest(context, tmpDir)
                else                  -> null
            }
        }

        suspend fun executeImport(
            context: Context,
            session: Long,
            content: Iterable<BackupItem>,
            onUpdateProgress: (CharSequence) -> Unit,
        ) {
            val tmpDir = SessionManger.sessionDirectory(session)
            val manifest = readManifest(context, session)
            require(manifest != null) { "No Manifest!" }
            // filter
            val selected = manifest.files.filterKeys { it in content }
            for ((item, relativePath) in selected) {
                onUpdateProgress(displayName(item, context.resources))
                File(tmpDir, relativePath).source().use { source ->

                    val executor = executor(item)
                    executor?.import(context, source) ?: warning(context, TAG, "Could not import $item")
                }
            }
        }

        fun endImportBackupFromArchive(session: Long) {
            SessionManger.terminateSession(session)
        }

        private fun decodeManifest(inputFile: File): BackupManifestFile {
            val manifestFile = inputFile.source().buffer().use {
                val raw = it.readUtf8()
                parser.decodeFromString<BackupManifestFile>(raw)
            }
            return manifestFile
        }

        private fun guessManifest(context: Context, dir: File): BackupManifestFile? {
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
            warning(context, TAG, "Couldn't analysis the content of this backup")
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