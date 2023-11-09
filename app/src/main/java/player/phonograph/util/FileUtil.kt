/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import kotlin.math.log10
import kotlin.math.pow
import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.DecimalFormat
import java.util.LinkedList

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object FileUtil {
    private const val TAG = "FileUtil"
    /**
     * create the file or delete it and create new one if exists
     */
    fun File.createOrOverrideFile(): File {
        if (exists()) delete()
        createNewFile()
        return this
    }

    fun moveFile(from: File, to: File) {
        require(from.exists()) { "${from.path} doesn't exits!" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } else {
            if (from.canWrite()) {
                if (to.exists()) {
                    to.delete().also { require(it) { "Can't delete ${to.path}" } }
                }
                from.renameTo(to).also { require(it) { "Restore ${from.path} failed!" } }
            }
        }
    }

    private fun makePlaceholders(len: Int): String {
        val sb = StringBuilder(len * 2 - 1)
        sb.append("?")
        for (i in 1 until len) {
            sb.append(",?")
        }
        return sb.toString()
    }

    private fun toPathArray(files: List<File>): Array<String> =
        files.map { safeGetCanonicalPath(it) }.toTypedArray()

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


    /**
     * save [content] to a file from document uri ([dest])
     */
    fun saveToFile(dest: Uri, content: String, contentResolver: ContentResolver) {
        contentResolver.openFileDescriptor(dest, "wt")?.use { descriptor ->
            FileOutputStream(descriptor.fileDescriptor).use { stream ->
                stream.bufferedWriter().use {
                    it.write(content)
                    it.flush()
                }
            }
        }
    }
}
