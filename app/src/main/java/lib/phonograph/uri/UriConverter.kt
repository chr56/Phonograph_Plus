/*
 * Copyright (c) 2022~2023 chr_56
 */

package lib.phonograph.uri

import lib.phonograph.storage.getBasePath
import lib.phonograph.storage.getStorageId
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

const val DOCUMENT_PROVIDER_PATH_TREE = "tree"
const val DOCUMENT_PROVIDER_PATH_DOCUMENT = "document"

/**
 * Build DocumentProvider Document Uri:
 *
 * `content://com.android.externalstorage.documents/document/<DocumentSchemedPath>`
 *
 * @param file path of uri
 */
fun guessDocumentUri(
    context: Context,
    file: File,
    id: String? = null,
): Uri = basicConvertDocumentProviderUri(
    context,
    file,
    EXTERNAL_STORAGE_AUTHORITY,
    DOCUMENT_PROVIDER_PATH_DOCUMENT,
    id
)

/**
 * Build DocumentProvider DocumentTree Uri from file path:
 *
 * `content://com.android.externalstorage.documents/tree/<DocumentSchemedPath>`
 *
 * @param file path of uri
 */
fun guessTreeUri(
    context: Context,
    file: File,
    id: String? = null,
): Uri = basicConvertDocumentProviderUri(
    context,
    file,
    EXTERNAL_STORAGE_AUTHORITY,
    DOCUMENT_PROVIDER_PATH_TREE,
    id
)

/**
 * Build basic DocumentProvider Uri:
 *
 * `content://<PackageNameOfDocumentProvider>/<type>/<Path>`
 *
 * @param authority package name of document provider (like `com.android.externalstorage.documents`)
 * @param type See [DocumentsContract] Constant (like `document`, `tree`)
 */
private fun basicConvertDocumentProviderUri(
    context: Context,
    file: File,
    authority: String,
    type: String,
    id: String?,
): Uri {
    val storageId = file.getStorageId(context)
    val basePath = file.getBasePath()
    require(storageId.isNotEmpty() && basePath.isNotEmpty()) { "Invalid path: ${file.absoluteFile}" }

    val location = "$storageId:$basePath"

    return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority)
        .appendPath(type).appendPath(location)
        .let { if (id != null) it.appendPath(id) else it }
        .build()
}