/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util

import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import player.phonograph.BaseApp
import player.phonograph.notification.ErrorNotification
import java.io.*
import java.text.DecimalFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object FileUtil {

    fun listFiles(directory: File, fileFilter: FileFilter?): Array<File>? {
        return directory.listFiles(fileFilter)
    }

    fun listFilesDeep(directory: File, fileFilter: FileFilter?): List<File>? =
        rListFilesDeep(directory, fileFilter)

    fun listFilesDeep(files: Collection<File>, fileFilter: FileFilter?): List<File>? {
        val resFiles: MutableList<File> = LinkedList()
        for (file in files) {
            if (file.isDirectory) {
                resFiles.addAll(rListFilesDeep(file, fileFilter).orEmpty())
            } else if (fileFilter == null || fileFilter.accept(file)) {
                resFiles.add(file)
            }
        }
        return if (resFiles.isEmpty()) null else resFiles
    }

    private fun rListFilesDeep(directory: File, fileFilter: FileFilter?): List<File>? {
        val result: MutableList<File> = LinkedList()
        directory.listFiles(fileFilter)?.let { files ->
            for (file in files) {
                if (file.isDirectory) {
                    result.addAll(rListFilesDeep(file, fileFilter).orEmpty())
                } else {
                    result.add(file)
                }
            }
        }
        return if (result.isEmpty()) null else result
    }

    fun File.mimeTypeIs(mimeType: String): Boolean {
        return if (mimeType.isEmpty() || mimeType == "*/*") {
            true
        } else {
            // get the file mime type
            val filename = this.toURI().toString()
            val dotPos = filename.lastIndexOf('.')
            if (dotPos == -1) {
                return false
            }
            val fileExtension = filename.substring(dotPos + 1).lowercase()
            val fileType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension) ?: return false
            // check the 'type/subtype' pattern
            if (fileType == mimeType) {
                return true
            }
            // check the 'type/*' pattern
            val mimeTypeDelimiter = mimeType.lastIndexOf('/')
            if (mimeTypeDelimiter == -1) {
                return false
            }
            val mimeTypeMainType = mimeType.substring(0, mimeTypeDelimiter)
            val mimeTypeSubtype = mimeType.substring(mimeTypeDelimiter + 1)
            if (mimeTypeSubtype != "*") {
                return false
            }
            val fileTypeDelimiter = fileType.lastIndexOf('/')
            if (fileTypeDelimiter == -1) {
                return false
            }
            val fileTypeMainType = fileType.substring(0, fileTypeDelimiter)
            fileTypeMainType == mimeTypeMainType
        }
    }

    fun stripExtension(str: String): String {
        val pos = str.lastIndexOf('.')
        return if (pos == -1) str else str.substring(0, pos)
    }

    @Throws(Exception::class)
    fun readFromStream(`is`: InputStream?): String {
        val reader = BufferedReader(InputStreamReader(`is`))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (sb.isNotEmpty()) {
                sb.append("\n")
            }
            sb.append(line)
        }
        reader.close()
        return sb.toString()
    }

    @Throws(Exception::class)
    fun read(file: File): String {
        val fin = FileInputStream(file)
        val ret = readFromStream(fin)
        fin.close()
        return ret
    }

    @JvmStatic
    fun safeGetCanonicalPath(file: File): String {
        return try {
            file.canonicalPath
        } catch (e: IOException) {
            e.printStackTrace()
            file.absolutePath
        }
    }

    @JvmStatic
    fun safeGetCanonicalFile(file: File): File {
        return try {
            file.canonicalFile
        } catch (e: IOException) {
            e.printStackTrace()
            file.absoluteFile
        }
    }

    fun getReadableFileSize(size: Long): String {
        if (size <= 0) return "$size B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups =
            (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.##").format(
            size / 1024.0.pow(digitGroups.toDouble())
        ) + " " + units[digitGroups]
    }


    // root
    val defaultStartDirectory: File
        get() {
            val musicDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC
            )

            return if (musicDir != null && musicDir.exists() && musicDir.isDirectory) {
                musicDir
            } else {
                val externalStorage = Environment.getExternalStorageDirectory()
                if (externalStorage.exists() && externalStorage.isDirectory) {
                    externalStorage
                } else {
                    BaseApp.instance.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File("/") // root
                }
            }
        }

    class DirectoryInfo(val file: File, val fileFilter: FileFilter)
    object FileScanner {
        fun listPaths(
            directoryInfos: DirectoryInfo,
            scope: CoroutineScope,
            recursive: Boolean = false,
        ): Array<String>? {
            if (!scope.isActive) return null

            val paths: Array<String>? =
                try {
                    if (directoryInfos.file.isDirectory) {
                        if (!scope.isActive) return null
                        // todo
                        val files =
                            if (recursive) {
                                FileUtil.listFilesDeep(
                                    directoryInfos.file,
                                    directoryInfos.fileFilter
                                )
                            } else {
                                FileUtil.listFiles(directoryInfos.file, directoryInfos.fileFilter)?.toList()
                            }

                        if (files.isNullOrEmpty()) return null
                        Array(files.size) { i ->
                            if (!scope.isActive) return null
                            FileUtil.safeGetCanonicalPath(files[i])
                        }
                    } else {
                        arrayOf(FileUtil.safeGetCanonicalPath(directoryInfos.file))
                    }.also {
                        Log.v("FileScanner", "success")
                    }
                } catch (e: Exception) {
                    ErrorNotification.postErrorNotification(e, "Fail to Load files!")
                    Log.w("FolderFragment", e)
                    null
                }
            return paths
        }

        val audioFileFilter: FileFilter =
            FileFilter { file: File ->
                !file.isHidden && (
                        file.isDirectory ||
                                file.mimeTypeIs("audio/*") ||
                                file.mimeTypeIs("application/ogg")
                        )
            }
    }
}