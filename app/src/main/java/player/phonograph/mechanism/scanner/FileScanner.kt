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
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileFilter

object FileScanner {

    private const val TAG = "FileScanner"

    suspend fun listPaths(
        context: Context,
        directoryInfo: DirectoryInfo,
        recursive: Boolean = false,
    ): Array<String>? = coroutineScope {
        try {
            val files = if (directoryInfo.file.isDirectory) {
                if (recursive) {
                    listFilesDeep(directoryInfo.file, directoryInfo.fileFilter)
                } else {
                    listFiles(directoryInfo.file, directoryInfo.fileFilter)
                }
            } else {
                listOf(directoryInfo.file)
            }

            if (files.isEmpty()) return@coroutineScope null

            yield()

            files.map { file -> safeGetCanonicalPath(file) }.toTypedArray()

        } catch (e: Exception) {
            warning(context, TAG, "Failed to list files!", e)
            null
        }
    }

    val audioFileFilter: FileFilter = FileFilter { file: File ->
        !file.isHidden && (file.isDirectory || file.mimeTypeIs("audio/*") || file.mimeTypeIs("application/ogg"))
    }

    private fun listFiles(directory: File, fileFilter: FileFilter?): List<File> {
        val files: Array<File>? = directory.listFiles(fileFilter)
        return if (files.isNullOrEmpty()) emptyList() else files.toList()
    }

    private suspend fun listFilesDeep(directory: File, fileFilter: FileFilter?): List<File> {
        val result = mutableListOf<File>()
        val files = directory.listFiles(fileFilter) ?: return emptyList()
        yield()
        for (file in files) {
            if (file.isDirectory) {
                result.addAll(listFilesDeep(file, fileFilter))
            } else {
                result.add(file)
            }
        }
        return if (result.isEmpty()) emptyList() else result
    }
}