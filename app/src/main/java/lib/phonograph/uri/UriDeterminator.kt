/*
 * Copyright (c) 2022~2023 chr_56
 */

package lib.phonograph.uri

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore


const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

fun Uri.isDocumentProviderUri(): Boolean =
    authority == EXTERNAL_STORAGE_AUTHORITY

fun Uri.isDocumentProviderUriSafe(context: Context): Boolean =
    DocumentsContract.isDocumentUri(context, this)

fun Uri.isTreeDocumentFile(): Boolean =
    isDocumentProviderUri() && path?.startsWith("/tree/") == true

fun Uri.isTreeDocumentFileSafe(): Boolean =
    DocumentsContract.isTreeUri( this)

/*
File picker for each API version gives the following URIs:
* API 26 - 27 => content://com.android.providers.downloads.documents/document/22
* API 28 - 29 => content://com.android.providers.downloads.documents/document/raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2Fscreenshot.jpeg
* API 30+     => content://com.android.providers.downloads.documents/document/msf%3A42
 */
const val DOWNLOADS_FOLDER_AUTHORITY = "com.android.providers.downloads.documents"

/**
 * Only available on API 26 to 29.
 */
const val DOWNLOADS_TREE_URI = "content://$DOWNLOADS_FOLDER_AUTHORITY/tree/downloads"

const val MEDIA_FOLDER_AUTHORITY = "com.android.providers.media.documents"

fun Uri.isDownloadsDocument(): Boolean = authority == DOWNLOADS_FOLDER_AUTHORITY

fun Uri.isMediaDocument(): Boolean = authority == MEDIA_FOLDER_AUTHORITY

fun Uri.isRawFile(): Boolean = scheme == ContentResolver.SCHEME_FILE

fun Uri.isMediaFile(): Boolean = authority == MediaStore.AUTHORITY