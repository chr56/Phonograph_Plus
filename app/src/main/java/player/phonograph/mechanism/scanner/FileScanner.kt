/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.scanner

import player.phonograph.model.DirectoryInfo
import player.phonograph.util.FileUtil
import player.phonograph.util.FileUtil.mimeTypeIs
import player.phonograph.util.reportError
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileFilter

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object FileScanner {

    private const val TAG = "FileScanner"

    suspend fun listPaths(
        directoryInfos: DirectoryInfo,
        recursive: Boolean = false,
    ): Array<String>? = coroutineScope {
          try {
              if (directoryInfos.file.isDirectory) {
                  if (!isActive) return@coroutineScope null
                  // todo
                  val files =
                      if (recursive) {
                          FileUtil.listFilesDeep(directoryInfos.file, directoryInfos.fileFilter)
                      } else {
                          FileUtil.listFiles(directoryInfos.file, directoryInfos.fileFilter)?.toList()
                      }

                  if (files.isNullOrEmpty()) return@coroutineScope null

                  Array(files.size) { i ->
                      if (!isActive) return@coroutineScope null
                      FileUtil.safeGetCanonicalPath(files[i])
                  }
              } else {
                  arrayOf(FileUtil.safeGetCanonicalPath(directoryInfos.file))
              }
          } catch (e: Exception) {
              reportError(e, TAG, "Fail to Load files!")
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
}