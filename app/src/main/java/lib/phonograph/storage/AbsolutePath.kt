/*
 * Copyright (c) 2022~2023 chr_56
 */

package lib.phonograph.storage

import androidx.documentfile.provider.DocumentFile
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * * For file in SD Card => `/storage/6881-2249/Music/song.mp3`
 * * For file in external storage => `/storage/emulated/0/Music/song.mp3`
 *
 * If you want to remember file locations in database or preference, please use this function.
 * When you reopen the file, just call [fromFullPath]
 *
 * @return File's actual path. Returns empty `String` if:
 * * It is not a raw file and the authority is neither [EXTERNAL_STORAGE_AUTHORITY] nor [DOWNLOADS_FOLDER_AUTHORITY]
 * * The authority is [DOWNLOADS_FOLDER_AUTHORITY], but [isTreeDocumentFile] returns `false`
 *
 * @see File.getAbsolutePath
 * @see getBasePath
 */
@Suppress("DEPRECATION")
fun DocumentFile.getAbsolutePath(context: Context): String {
    val path = uri.path.orEmpty()
    val storageID = uri.getStorageId(context)
    return when {
        uri.isRawFile()                                                                                   -> path

        uri.isDocumentProviderUri() && path.contains("/document/$storageID:")                             -> {
            val basePath = path.substringAfterLast("/document/$storageID:", "").trim('/')
            if (storageID == PRIMARY) {
                "$externalStoragePath/$basePath".trimEnd('/')
            } else {
                "/storage/$storageID/$basePath".trimEnd('/')
            }
        }

        uri.toString().let { it == DOWNLOADS_TREE_URI || it == "$DOWNLOADS_TREE_URI/document/downloads" } ->
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath

        uri.isDownloadsDocument()                                                                         -> {
            when {
                // API 26 - 27 => content://com.android.providers.downloads.documents/document/22
                Build.VERSION.SDK_INT < Build.VERSION_CODES.P && path.matches(Regex("/document/\\d+"))           -> {

                    val fileName =
                        context.contentResolver.query(
                            uri,
                            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                                if (columnIndex != -1) {
                                    return@use cursor.getString(columnIndex)
                                }
                            }
                            return@use null
                        } ?: ""

                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        fileName
                    ).absolutePath
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && path.matches(Regex("(.*?)/ms[f,d]:\\d+(.*?)")) -> {
                    if (uri.isTreeDocumentFile()) {
                        val parentTree = mutableListOf(name.orEmpty())
                        var parent = this
                        while (parent.parentFile?.also { parent = it } != null) {
                            parentTree.add(parent.name.orEmpty())
                        }
                        "$externalStoragePath/${
                            parentTree.reversed().joinToString("/")
                        }".trimEnd('/')
                    } else {
                        // we can't use msf/msd ID as MediaFile ID to fetch relative path, so just return empty String
                        ""
                    }
                }

                else                                                                                             -> path.substringAfterLast(
                    "/document/raw:",
                    ""
                ).trimEnd('/')
            }
        }

        !uri.isTreeDocumentFile()                                                                         -> ""
        inPrimaryStorage(context)                                                                         -> "$externalStoragePath/${
            getBasePath(
                context
            )
        }".trimEnd('/')
        else                                                                                              -> "/storage/$storageID/${
            getBasePath(
                context
            )
        }".trimEnd('/')
    }
}


fun DocumentFile.inPrimaryStorage(context: Context) = uri.isTreeDocumentFile() && uri.getStorageId(context) == PRIMARY ||
        uri.isRawFile() && uri.path.orEmpty().startsWith(externalStoragePath)

