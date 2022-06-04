
/*
 * Copyright (C) 2021 Anggrayudi H
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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.File

fun File.getStorageId(context: Context) = when {
    path.startsWith(externalStoragePath) -> PRIMARY
    path.startsWith(context.dataDir.path) -> DATA
    else -> path.substringAfter("/storage/", "").substringBefore('/')
}

/**
 * If given [Uri] with path `/tree/primary:Downloads/MyVideo.mp4`, then return `primary`.
 */
fun Uri.getStorageId(context: Context): String {
    val path = path.orEmpty()
    return if (isRawFile) {
        File(path).getStorageId(context)
    } else when {
        isExternalStorageDocument -> path.substringBefore(':', "").substringAfterLast('/')
        isDownloadsDocument -> PRIMARY
        else -> ""
    }
}

val Uri.isTreeDocumentFile: Boolean
    get() = path?.startsWith("/tree/") == true

val Uri.isExternalStorageDocument: Boolean
    get() = authority == EXTERNAL_STORAGE_AUTHORITY

val Uri.isDownloadsDocument: Boolean
    get() = authority == DOWNLOADS_FOLDER_AUTHORITY

val Uri.isMediaDocument: Boolean
    get() = authority == MEDIA_FOLDER_AUTHORITY

val Uri.isRawFile: Boolean
    get() = scheme == ContentResolver.SCHEME_FILE

val Uri.isMediaFile: Boolean
    get() = authority == MediaStore.AUTHORITY

const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

/*
File picker for each API version gives the following URIs:
* API 26 - 27 => content://com.android.providers.downloads.documents/document/22
* API 28 - 29 => content://com.android.providers.downloads.documents/document/raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2Fscreenshot.jpeg
* API 30+     => content://com.android.providers.downloads.documents/document/msf%3A42
 */
const val DOWNLOADS_FOLDER_AUTHORITY = "com.android.providers.downloads.documents"

const val MEDIA_FOLDER_AUTHORITY = "com.android.providers.media.documents"

/**
 * Only available on API 26 to 29.
 */
const val DOWNLOADS_TREE_URI = "content://$DOWNLOADS_FOLDER_AUTHORITY/tree/downloads"

fun DocumentFile.inPrimaryStorage(context: Context) = uri.isTreeDocumentFile && getStorageId(context) == PRIMARY ||
    uri.isRawFile && uri.path.orEmpty().startsWith(externalStoragePath)
