/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

@file:Suppress("DEPRECATION")

package player.phonograph.util

import android.content.Context
import android.provider.MediaStore.Audio.AudioColumns.DATA
import android.webkit.MimeTypeMap
import java.io.*
import java.text.DecimalFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow
import player.phonograph.mediastore.SongLoader.getSongs
import player.phonograph.mediastore.SongLoader.makeSongCursor
import lib.phonograph.misc.SortedCursor
import player.phonograph.model.Song

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object FileUtil {

    fun matchFilesWithMediaStore(context: Context, files: List<File>): List<Song> {
        return getSongs(makeSongCursor(context, files))
    }

    private fun makeSongCursor(context: Context, files: List<File>): SortedCursor? {
        var selection: String? = null
        val paths: Array<String> = toPathArray(files)
        if (files.size in 1..998) { // 999 is the max amount Androids SQL implementation can handle.
            selection = "$DATA IN (${makePlaceholders(files.size)})"
        }
        val songCursor = makeSongCursor(context, selection, if (selection == null) null else paths)
        return if (songCursor == null) null else SortedCursor(songCursor, paths, DATA)
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

    fun fileIsMimeType(file: File, mimeType: String?, mimeTypeMap: MimeTypeMap): Boolean {
        return if (mimeType == null || mimeType == "*/*") {
            true
        } else {
            // get the file mime type
            val filename = file.toURI().toString()
            val dotPos = filename.lastIndexOf('.')
            if (dotPos == -1) {
                return false
            }
            val fileExtension = filename.substring(dotPos + 1).lowercase()
            val fileType = mimeTypeMap.getMimeTypeFromExtension(fileExtension) ?: return false
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
}
