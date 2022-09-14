/*
 * Copyright © 2020-2022 Anggrayudi Hardiannico A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lib.phonograph.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.File

fun DocumentFile.getStorageId(context: Context) = uri.getStorageId(context)

/**
 * * For file in SD Card => `/storage/6881-2249/Music/song.mp3`
 * * For file in external storage => `/storage/emulated/0/Music/song.mp3`
 *
 * If you want to remember file locations in database or preference, please use this function.
 * When you reopen the file, just call [DocumentFileCompat.fromFullPath]
 *
 * @return File's actual path. Returns empty `String` if:
 * * It is not a raw file and the authority is neither [DocumentFileCompat.EXTERNAL_STORAGE_AUTHORITY] nor [DocumentFileCompat.DOWNLOADS_FOLDER_AUTHORITY]
 * * The authority is [DocumentFileCompat.DOWNLOADS_FOLDER_AUTHORITY], but [isTreeDocumentFile] returns `false`
 *
 * @see File.getAbsolutePath
 * @see getSimplePath
 */
@Suppress("DEPRECATION")
fun DocumentFile.getAbsolutePath(context: Context): String {
    val path = uri.path.orEmpty()
    val storageID = getStorageId(context)
    return when {
        uri.isRawFile -> path

        uri.isExternalStorageDocument && path.contains("/document/$storageID:") -> {
            val basePath = path.substringAfterLast("/document/$storageID:", "").trim('/')
            if (storageID == PRIMARY) {
                "$externalStoragePath/$basePath".trimEnd('/')
            } else {
                "/storage/$storageID/$basePath".trimEnd('/')
            }
        }

        uri.toString().let { it == DOWNLOADS_TREE_URI || it == "$DOWNLOADS_TREE_URI/document/downloads" } ->
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath

        uri.isDownloadsDocument -> {
            when {
                // API 26 - 27 => content://com.android.providers.downloads.documents/document/22
                Build.VERSION.SDK_INT < Build.VERSION_CODES.P && path.matches(Regex("/document/\\d+")) -> {

                    val fileName =
                        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                                if (columnIndex != -1) {
                                    return@use cursor.getString(columnIndex)
                                }
                            }
                            return@use null
                        } ?: ""

                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName).absolutePath
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && path.matches(Regex("(.*?)/ms[f,d]:\\d+(.*?)")) -> {
                    if (uri.isTreeDocumentFile) {
                        val parentTree = mutableListOf(name.orEmpty())
                        var parent = this
                        while (parent.parentFile?.also { parent = it } != null) {
                            parentTree.add(parent.name.orEmpty())
                        }
                        "$externalStoragePath/${parentTree.reversed().joinToString("/")}".trimEnd('/')
                    } else {
                        // we can't use msf/msd ID as MediaFile ID to fetch relative path, so just return empty String
                        ""
                    }
                }

                else -> path.substringAfterLast("/document/raw:", "").trimEnd('/')
            }
        }

        !uri.isTreeDocumentFile -> ""
        inPrimaryStorage(context) -> "$externalStoragePath/${getBasePath(context)}".trimEnd('/')
        else -> "/storage/$storageID/${getBasePath(context)}".trimEnd('/')
    }
}

/**
 * @return File path without storage ID. Returns empty `String` if:
 * * It is the root path
 * * It is not a raw file and the authority is neither [DocumentFileCompat.EXTERNAL_STORAGE_AUTHORITY] nor [DocumentFileCompat.DOWNLOADS_FOLDER_AUTHORITY]
 * * The authority is [DocumentFileCompat.DOWNLOADS_FOLDER_AUTHORITY], but [isTreeDocumentFile] returns `false`
 */
@Suppress("DEPRECATION")
fun DocumentFile.getBasePath(context: Context): String {
    val path = uri.path.orEmpty()
    val storageID = getStorageId(context)
    return when {
        uri.isRawFile -> File(path).getBasePath(context)

        uri.isExternalStorageDocument && path.contains("/document/$storageID:") -> {
            path.substringAfterLast("/document/$storageID:", "").trim('/')
        }

        uri.isDownloadsDocument -> {
            // content://com.android.providers.downloads.documents/tree/raw:/storage/emulated/0/Download/Denai/document/raw:/storage/emulated/0/Download/Denai
            // content://com.android.providers.downloads.documents/tree/downloads/document/raw:/storage/emulated/0/Download/Denai
            when {
                // API 26 - 27 => content://com.android.providers.downloads.documents/document/22
                Build.VERSION.SDK_INT < Build.VERSION_CODES.P && path.matches(Regex("/document/\\d+")) -> {
                    val fileName =
                        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
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
                    if (uri.isTreeDocumentFile) {
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

                else -> path.substringAfterLast(externalStoragePath, "").trim('/')
            }
        }
        else -> ""
    }
}

/**
 * @param fullPath For SD card can be full path `storage/6881-2249/Music` or simple path `6881-2249:Music`.
 *             For primary storage can be `/storage/emulated/0/Music` or simple path `primary:Music`.
 * @return Given `storage/6881-2249/Music/My Love.mp3`, then return `Music/My Love.mp3`.
 *          May return empty `String` if it is a root path of the storage.
 */

fun getBasePath(context: Context, fullPath: String): String {
    val basePath = if (fullPath.startsWith('/')) {
        val dataDir = context.dataDir.path
        val externalStoragePath = externalStoragePath
        when {
            fullPath.startsWith(externalStoragePath) -> fullPath.substringAfter(externalStoragePath)
            fullPath.startsWith(dataDir) -> fullPath.substringAfter(dataDir)
            else -> fullPath.substringAfter("/storage/", "").substringAfter('/', "")
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
