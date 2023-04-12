/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.migrate.backup

import player.phonograph.util.FileUtil.createOrOverrideFile
import player.phonograph.util.text.currentTimestamp
import player.phonograph.util.transferToOutputStream
import player.phonograph.util.zip.ZipUtil.addToZipFile
import player.phonograph.util.zip.ZipUtil.extractZipFile
import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object Backup {
    // todo
    val DEFAULT_BACKUP_CONFIG =
        listOf(
            SettingBackup, FavoriteBackup, PathFilterBackup, PlayingQueuesBackup,
            FavoriteDatabaseBackup,
            PathFilterDatabaseBackup,
            HistoryDatabaseBackup,
            SongPlayCountDatabaseBackup,
            MusicPlaybackStateDatabaseBackup,
        )

    fun exportBackupToArchive(
        context: Context,
        config: List<BackupItem> = DEFAULT_BACKUP_CONFIG,
        targetOutputStream: OutputStream,
    ) {

        val workingDir = workingDir(context)
        val tmpDir = tmpDir(workingDir, currentTimestamp())

        exportBackupToDirectory(context, config, tmpDir)

        ZipOutputStream(targetOutputStream).use { zip ->
            tmpDir.listFiles()?.forEach {
                addToZipFile(zip, it, it.name)
            }
        }

        tmpDir.deleteRecursively()
    }


    private fun exportBackupToDirectory(
        context: Context,
        config: List<BackupItem>,
        destination: File,
    ) {
        val timestamp = currentTimestamp()

        val fileList = mutableMapOf<BackupItem, String>() // track files added

        // export backups
        for (item in config) {
            val filename = "${item.key}_$timestamp.${item.type.suffix}"
            val file = File(destination, filename).createOrOverrideFile()
            file.outputStream().use { outputStream ->
                item.data(context).use { inputStream ->
                    inputStream.transferToOutputStream(outputStream)
                }
            }
            fileList[item] = filename
        }

        // generate manifest file
        val manifestFile = File(destination, ManifestFile.BACKUP_MANIFEST_FILENAME).createOrOverrideFile()
        manifestFile.outputStream().bufferedWriter().use {
            val manifest = ManifestFile(timestamp, fileList)
            val raw = parser.encodeToString(manifest)
            it.write(raw)
            it.flush()
        }
    }
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

    fun readManifest(
        session: Long,
    ): ManifestFile? {
        val tmpDir = SessionManger.directory(session)
        val manifestFile = File(tmpDir, ManifestFile.BACKUP_MANIFEST_FILENAME)
        return if (manifestFile.exists()) decodeManifest(manifestFile) else null
    }

    fun executeImport(
        context: Context,
        session: Long,
        content: Iterable<BackupItem>,
    ) {
        val tmpDir = SessionManger.directory(session)
        val manifest = readManifest(session) ?: throw Exception("No Manifest!")
        // filter
        val selected = manifest.files.filterKeys { it in content }
        for ((item, relativePath) in selected) {
            FileInputStream(File(tmpDir, relativePath)).use { inputStream ->
                item.import(inputStream, context)
            }
        }
    }

    fun endImportBackupFromArchive(
        session: Long,
    ) {
        SessionManger.terminateSession(session)
    }

    fun importBackupFromArchive(
        context: Context,
        sourceInputStream: InputStream,
    ) {
        val workingDir = workingDir(context)
        val tmpDir = tmpDir(workingDir, currentTimestamp())

        ZipInputStream(sourceInputStream).use { zipIn ->
            extractZipFile(zipIn, tmpDir)
            importBackupFromDirectory(context, tmpDir)
        }

        tmpDir.deleteRecursively()
    }

    private fun importBackupFromDirectory(
        context: Context,
        source: File,
    ) {
        require(source.exists() && source.isDirectory) { "${source.absolutePath} is not accessible directory!" }
        // read manifest
        val manifestFile = File(source, ManifestFile.BACKUP_MANIFEST_FILENAME)
        if (manifestFile.exists() && manifestFile.isFile) {
            val manifest = decodeManifest(manifestFile)
            for ((item, relativePath) in manifest.files) {
                FileInputStream(File(source, relativePath)).use { inputStream ->
                    item.import(inputStream, context)
                }
            }
        } else {
            throw IllegalStateException("No manifest found!") //todo: implement no-manifest-import
        }
    }

    private fun decodeManifest(inputFile: File): ManifestFile {
        val manifestFile = inputFile.inputStream().bufferedReader().use {
            val raw = it.readText()
            parser.decodeFromString<ManifestFile>(raw)
        }
        return manifestFile
    }

    private object SessionManger {
        private val sessions = mutableMapOf<Long, File>()
        private fun sessionFolderName(id: Long) = "${TMP_BACKUP_FOLDER_PREFIX}_$id"

        fun directory(sessionId: Long) = sessions[sessionId]

        fun newSession(context: Context): Pair<Long, File> {
            val sessionId = currentTimestamp()
            val tmpFile = File(workingDir(context), sessionFolderName(sessionId)).also { it.mkdirs() }
            sessions[sessionId] = tmpFile
            return sessionId to tmpFile
        }

        fun terminateSession(sessionId: Long) {
            val file = sessions.remove(sessionId)
            file?.deleteRecursively()
        }

    }

    private val parser by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            encodeDefaults = true
        }
    }

    private const val TMP_BACKUP_FOLDER_PREFIX = "BackupTmp"
    private fun workingDir(context: Context) = context.externalCacheDir ?: context.cacheDir!!
    private fun tmpDir(workingDir: File, session: Long) =
        File(workingDir, "${TMP_BACKUP_FOLDER_PREFIX}_${session}").also { it.mkdirs() }
}