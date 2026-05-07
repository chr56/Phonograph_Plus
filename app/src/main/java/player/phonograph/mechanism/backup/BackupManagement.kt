/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.backup

import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import player.phonograph.BuildConfig
import player.phonograph.foundation.error.warning
import player.phonograph.model.backup.BackupItem
import player.phonograph.model.backup.BackupItemExecutor
import player.phonograph.model.backup.BackupManifestFile
import player.phonograph.model.backup.BackupType
import player.phonograph.repo.database.store.HistoryStore.Companion.HISTORY_DB
import player.phonograph.repo.database.store.SongPlayCountStore.Companion.SONG_PLAY_COUNT_DB
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.service.queue.MusicPlaybackQueueStore.Companion.MUSIC_PLAYBACK_STATE_DB
import player.phonograph.ui.resource.Texts
import player.phonograph.util.text.currentTimestamp
import player.phonograph.util.zip.ZipUtil.extractDirectory
import player.phonograph.util.zip.ZipUtil.zipDirectory
import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object Backup {

    private fun executor(item: BackupItem): BackupItemExecutor? = when (item) {
        BackupItem.Settings              -> SettingsDataBackupItemExecutor
        BackupItem.PlayingQueues         -> PlayingQueuesDataBackupItemExecutor
        BackupItem.Favorites             -> FavoritesDataBackupItemExecutor
        BackupItem.InternalPlaylists     -> InternalDatabasePlaylistsDataBackupItemExecutor
        BackupItem.MainDatabase          -> RawDatabaseBackupItemExecutor(MusicDatabase.DATABASE_NAME)
        BackupItem.HistoryDatabase       -> RawDatabaseBackupItemExecutor(HISTORY_DB)
        BackupItem.SongPlayCountDatabase -> RawDatabaseBackupItemExecutor(SONG_PLAY_COUNT_DB)
        BackupItem.PlayingQueuesDatabase -> RawDatabaseBackupItemExecutor(MUSIC_PLAYBACK_STATE_DB)
        // Below are deprecated since 1102 (importing only)
        BackupItem.PathFilter            -> PathFilterDataBackupItemExecutor
        BackupItem.PathFilterDatabase    -> LegacyPathFilterDatabaseBackupItemExecutor
        BackupItem.FavoriteDatabase      -> LegacyFavoritesDatabaseBackupItemExecutor
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
                    val filename = "${item.serializationName}.${item.type.suffix}"
                    val path = destinationPath / filename
                    fs.write(path, mustCreate = true) {
                        exported.use { writeAll(it) }
                    }
                    files[item] = filename
                } else {
                    warning(context, TAG, "No content to export for ${item.serializationName}")
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
             // check
            require(manifest.phonographVersionCode >= 1082) {
                "Could not import backups created before v1.8.2! [current ${manifest.phonographVersion}]"
            }
            require(manifest.phonographVersionCode != 1110) {
                "Backups created by v1.11.0 are broken in nature!"
            }
            // filter
            val selected = manifest.files.filterKeys { it in content }
            for ((item, relativePath) in selected) {
                onUpdateProgress(Texts.backupItem(context.resources, item))
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
                for (item in BackupItem.entries) {
                    val expected = if (item.type == BackupType.DATABASE) {
                        item.serializationName.removePrefix(BackupItem.PREFIX_DATABASE)
                    } else {
                        item.serializationName
                    }
                    for (fileName in files) {
                        if (
                            fileName.endsWith(item.type.suffix, true) &&
                            fileName.startsWith(expected, ignoreCase = true)
                        ) {
                            map[item] = fileName
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

    private val parser
        get() = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            encodeDefaults = true
        }

    private const val TAG = "Backup"
}