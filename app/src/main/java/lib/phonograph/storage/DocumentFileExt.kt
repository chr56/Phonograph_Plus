/*
 * Copyright (c) 2022~2023 chr_56
 */

package lib.phonograph.storage

import lib.phonograph.uri.isDocumentProviderUri
import lib.phonograph.uri.isDownloadsDocument
import lib.phonograph.uri.isRawFile
import androidx.documentfile.provider.DocumentFile
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log

fun DocumentFile.getBasePath(context: Context): String? =
    uri.getBasePath(context)

fun DocumentFile.getAbsolutePath(context: Context): String? =
    uri.getAbsolutePath(context)


fun Uri.getBasePath(context: Context): String? {
    return when {
        isDocumentProviderUri() -> documentProviderUriBasePath(uri = this, context)
        isRawFile()             -> {
            val path = path ?: return null
            try {
                externalFileBashPath(path)
            } catch (e: IllegalArgumentException) {
                Log.e("Storage", "unsupported path: $path", e)
                null
            }
        }
        isDownloadsDocument()   -> parseDownloadUriBasePath(context, uri = this)
        else                    -> null
    }
}

fun Uri.getAbsolutePath(context: Context): String? {
    return when {
        isDocumentProviderUri() -> documentProviderUriAbsolutePath(uri = this, context)
        isRawFile()             -> path
        else                    -> null
    }
}

/**
 * @author Anggrayudi Hardiannico A.
 */
private fun parseDownloadUriBasePath(context: Context, uri: Uri): String? {
    val path = uri.path ?: return null
    // content://com.android.providers.downloads.documents/tree/raw:/storage/emulated/0/Download/Denai/document/raw:/storage/emulated/0/Download/Denai
    // content://com.android.providers.downloads.documents/tree/downloads/document/raw:/storage/emulated/0/Download/Denai
    return when {
        // API 26 - 27 => content://com.android.providers.downloads.documents/document/22
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P  -> {
            if (path.matches(Regex("/document/\\d+"))) {
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
            } else null
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
            Log.e("Storage", "unsupported path: $path")
            null
        }
        else                                           ->
            path.substringAfterLast(externalStoragePath, "").trim('/')
    }
}