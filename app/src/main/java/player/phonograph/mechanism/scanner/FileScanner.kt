/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.scanner

import player.phonograph.foundation.error.warning
import player.phonograph.model.DirectoryInfo
import player.phonograph.util.file.mimeTypeIs
import player.phonograph.util.file.safeGetCanonicalPath
import android.content.Context
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileFilter
import java.util.LinkedList

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object FileScanner {

    private const val TAG = "FileScanner"

    suspend fun listPaths(
        context: Context,
        directoryInfos: DirectoryInfo,
        recursive: Boolean = false,
    ): Array<String>? = coroutineScope {
        try {
            if (directoryInfos.file.isDirectory) {
                if (!isActive) return@coroutineScope null
                // todo
                val files =
                    if (recursive) {
                        listFilesDeep(directoryInfos.file, directoryInfos.fileFilter)
                    } else {
                        listFiles(directoryInfos.file, directoryInfos.fileFilter)?.toList()
                    }

                if (files.isNullOrEmpty()) return@coroutineScope null

                Array(files.size) { i ->
                    if (!isActive) return@coroutineScope null
                    safeGetCanonicalPath(files[i])
                }
            } else {
                arrayOf(safeGetCanonicalPath(directoryInfos.file))
            }
        } catch (e: Exception) {
            warning(context, TAG, "Fail to Load files!", e)
            null
        }
    }

    val audioFileFilter: FileFilter =
        FileFilter { file: File ->
            !file.isHidden && (
                    file.isDirectory ||
                            file.mimeTypeIs("audio/*") ||
                            file.mimeTypeIs("application/ogg")
                    )
        }


    private fun listFiles(directory: File, fileFilter: FileFilter?): Array<File>? {
        return directory.listFiles(fileFilter)
    }

    private fun listFilesDeep(directory: File, fileFilter: FileFilter?): List<File>? =
        rListFilesDeep(directory, fileFilter)

    private fun listFilesDeep(files: Collection<File>, fileFilter: FileFilter?): List<File>? {
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
}