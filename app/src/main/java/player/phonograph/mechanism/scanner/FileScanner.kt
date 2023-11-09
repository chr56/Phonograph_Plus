/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.scanner

import player.phonograph.model.DirectoryInfo
import player.phonograph.util.FileUtil
import player.phonograph.util.FileUtil.mimeTypeIs
import player.phonograph.util.reportError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileFilter

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object FileScanner {

    private const val TAG = "FileScanner"

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
                }
            } catch (e: Exception) {
                reportError(e, TAG, "Fail to Load files!")
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