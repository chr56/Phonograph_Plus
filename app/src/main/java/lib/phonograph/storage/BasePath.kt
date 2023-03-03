/*
 * Copyright (c) 2022~2023 chr_56
 */

package lib.phonograph.storage

import lib.phonograph.uri.isDocumentProviderUri
import lib.phonograph.uri.isDownloadsDocument
import lib.phonograph.uri.isRawFile
import lib.phonograph.uri.isTreeDocumentFile
import androidx.documentfile.provider.DocumentFile
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * @return File path without storage ID. Returns empty `String` if:
 * * It is the root path
 * * It is not a raw file and the authority is neither [EXTERNAL_STORAGE_AUTHORITY] nor [DOWNLOADS_FOLDER_AUTHORITY]
 * * The authority is [DOWNLOADS_FOLDER_AUTHORITY], but [isTreeDocumentFile] returns `false`
 */
@Suppress("DEPRECATION")
fun DocumentFile.getBasePath(context: Context): String {
    val path = uri.path.orEmpty()
    val storageID = uri.getStorageId(context)
    return when {
        uri.isRawFile()                                                       -> File(path).getBasePath(context)

        uri.isDocumentProviderUri() && path.contains("/document/$storageID:") -> {
            path.substringAfterLast("/document/$storageID:", "").trim('/')
        }

        uri.isDownloadsDocument()                                             -> {
            // content://com.android.providers.downloads.documents/tree/raw:/storage/emulated/0/Download/Denai/document/raw:/storage/emulated/0/Download/Denai
            // content://com.android.providers.downloads.documents/tree/downloads/document/raw:/storage/emulated/0/Download/Denai
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
                    "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && path.matches(Regex("(.*?)/ms[f,d]:\\d+(.*?)")) -> {
                    if (uri.isTreeDocumentFile()) {
                        val parentTree = mutableListOf(name.orEmpty())
                        var parent = this
                        while (parent.parentFile?.also { parent = it } != null) {
                            parentTree.add(parent.name.orEmpty())
                        }
                        parentTree.reversed().joinToString("/")
                    } else {
                        // we can't use msf/msd ID as MediaFile ID to fetch relative path, so just return empty String
                        ""
                    }
                }

                else                                                                                             -> path.substringAfterLast(
                    externalStoragePath, ""
                ).trim('/')
            }
        }
        else                                                                  -> ""
    }
}

/**
 * @param fullPath absolute path.
 * @return relative path from root of StorageVolume.
 */
fun getBasePath(context: Context, fullPath: String): String {
    val basePath = if (fullPath.startsWith('/')) {
        val dataDir = context.dataDir.path
        val externalStoragePath = externalStoragePath
        when {
            fullPath.startsWith(externalStoragePath) -> fullPath.substringAfter(externalStoragePath)
            fullPath.startsWith(dataDir)             -> fullPath.substringAfter(dataDir)
            else                                     -> fullPath.substringAfter("/storage/", "").substringAfter('/', "")
        }
    } else {
        fullPath.substringAfter(':', "")
    }
    return basePath.trim('/') // .removeForbiddenCharsFromFilename()
}

fun File.getBasePath(context: Context): String {
    val externalStoragePath = externalStoragePath
    if (path.startsWith(externalStoragePath)) {
        return path.substringAfter(externalStoragePath, "").trim('/')
    }
    val dataDir = context.dataDir.path
    if (path.startsWith(dataDir)) {
        return path.substringAfter(dataDir, "").trim('/')
    }
    val storageId = getStorageId(context)
    return path.substringAfter("/storage/$storageId", "").trim('/')
}