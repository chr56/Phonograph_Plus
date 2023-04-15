/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.backup

import player.phonograph.util.FileUtil.createOrOverrideFile
import player.phonograph.util.text.currentTimestamp
import player.phonograph.util.transferToOutputStream
import player.phonograph.util.warning
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

    object Export {

        fun exportBackupToArchive(
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


        private fun exportBackupToDirectory(
            context: Context,
            config: List<BackupItem>,
            destination: File,
        ) {
            val timestamp = currentTimestamp()

            val fileList = mutableMapOf<BackupItem, String>() // track files added

            // export backups
            for (item in config) {
                val filename = "${item.key}.${item.type.suffix}"
                val exported = item.data(context)
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
            val manifestFile = File(destination, ManifestFile.BACKUP_MANIFEST_FILENAME).createOrOverrideFile()
            manifestFile.outputStream().bufferedWriter().use {
                val manifest = ManifestFile(timestamp, fileList)
                val raw = parser.encodeToString(manifest)
                it.write(raw)
                it.flush()
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

        fun readManifest(
            session: Long,
        ): ManifestFile? {
            val tmpDir = SessionManger.sessionDirectory(session)
            val manifestFile = File(tmpDir, ManifestFile.BACKUP_MANIFEST_FILENAME)
            return if (manifestFile.exists()) decodeManifest(manifestFile) else null
        }

        fun executeImport(
            context: Context,
            session: Long,
            content: Iterable<BackupItem>,
            onUpdateProgress: (CharSequence) -> Unit,
        ) {
            val tmpDir = SessionManger.sessionDirectory(session)
            val manifest = readManifest(session) ?: throw Exception("No Manifest!")
            // filter
            val selected = manifest.files.filterKeys { it in content }
            for ((item, relativePath) in selected) {
                onUpdateProgress(item.displayName(context.resources))
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

        private fun decodeManifest(inputFile: File): ManifestFile {
            val manifestFile = inputFile.inputStream().bufferedReader().use {
                val raw = it.readText()
                parser.decodeFromString<ManifestFile>(raw)
            }
            return manifestFile
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