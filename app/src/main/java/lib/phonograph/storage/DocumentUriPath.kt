/*
 * Copyright (c) 2022~2023 chr_56
 */

package lib.phonograph.storage

import lib.phonograph.uri.DOCUMENT_PROVIDER_PATH_DOCUMENT
import lib.phonograph.uri.DOCUMENT_PROVIDER_PATH_TREE
import lib.phonograph.uri.EXTERNAL_STORAGE_AUTHORITY
import lib.phonograph.uri.isDocumentProviderUri
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log

/**
 * @param uri system DocumentProviderUri (`content://com.android.externalstorage.documents/...`)
 * @return base path (relative file path from _the root of a storage volume_)
 */
fun documentProviderUriBasePath(uri: Uri, context: Context): String? {
    if (uri.authority != EXTERNAL_STORAGE_AUTHORITY) {
        Log.w("Storage", "Non-Android DocumentProvider: $uri")
    }
    return when {
        DocumentsContract.isTreeUri(uri)              -> documentTreeUriBasePath(uri)
        DocumentsContract.isDocumentUri(context, uri) -> documentUriBasePath(uri)
        else                                          -> childDocumentUriBasePath(uri) // may be a ChildDocumentUri
    }
}

/**
 * @param uri system DocumentProviderUri (`content://com.android.externalstorage.documents/...`)
 * @return base path (relative file path from _the root of a storage volume_)
 */
fun documentProviderUriBasePathForce(uri: Uri): String? {
    return if (uri.authority == EXTERNAL_STORAGE_AUTHORITY) childDocumentUriBasePath(uri) else null
}

/**
 * @param uri system Document Uri (`content://com.android.externalstorage.documents/document/<StorageVolume>:<BasePath>#...`)
 * @return base path (relative file path from _the root of a storage volume_)
 * @see documentTreeUriBasePath
 * @see documentUriBasePath
 * @see childDocumentUriBasePath
 */
internal fun documentUriBasePath(uri: Uri): String? {
    val map = parseUriPathPart(uri)
    return parseBasePath(map, DOCUMENT_PROVIDER_PATH_DOCUMENT)
}

/**
 * @param uri system Tree Document Uri (`content://com.android.externalstorage.documents/tree/<StorageVolume>:<BasePath>#...`)
 * @return base path (relative file path from _the root of a storage volume_)
 * @see documentTreeUriBasePath
 * @see documentUriBasePath
 * @see childDocumentUriBasePath
 */
internal fun documentTreeUriBasePath(uri: Uri): String? {
    val map = parseUriPathPart(uri)
    return parseBasePath(map, DOCUMENT_PROVIDER_PATH_TREE)
}

/**
 * @param uri system Child Document Uri (`content://com.android.externalstorage.documents/tree/<StorageVolume>:<BasePath>/document/<StorageVolume>:<BasePath>#...`)
 * @return base path (relative file path from _the root of a storage volume_)
 * @see documentTreeUriBasePath
 * @see documentUriBasePath
 * @see childDocumentUriBasePath
 */
private fun childDocumentUriBasePath(uri: Uri): String? {
    val map = parseUriPathPart(uri)
    val document = parseBasePath(map, DOCUMENT_PROVIDER_PATH_DOCUMENT)
    val tree = parseBasePath(map, DOCUMENT_PROVIDER_PATH_TREE)
    return document ?: tree
}


/**
 * @param uri system DocumentProviderUri (`content://com.android.externalstorage.documents/...`)
 * @return absolute path
 */
fun documentProviderUriAbsolutePath(uri: Uri, context: Context): String? {
    if (uri.authority != EXTERNAL_STORAGE_AUTHORITY) {
        Log.w("Storage", "Non-Android DocumentProvider: $uri")
    }
    return when {
        DocumentsContract.isTreeUri(uri)              -> documentTreeUriAbsolutePath(uri)
        DocumentsContract.isDocumentUri(context, uri) -> documentUriAbsolutePath(uri)
        else                                          -> childDocumentUriAbsolutePath(uri) // may be a ChildDocumentUri
    }
}

/**
 * @param uri system DocumentProviderUri (`content://com.android.externalstorage.documents/...`)
 * @return absolute (relative file path from _the root of a storage volume_)
 */
fun documentProviderUriAbsolutePathForce(uri: Uri): String? {
    return if (uri.authority == EXTERNAL_STORAGE_AUTHORITY) childDocumentUriAbsolutePath(uri) else null
}

/**
 * @param uri system Child Document Uri (`content://com.android.externalstorage.documents/document/<StorageVolume>:<BasePath>#...`)
 * @return absolute path
 * @see documentTreeUriAbsolutePath
 * @see documentUriAbsolutePath
 * @see childDocumentUriAbsolutePath
 */
internal fun documentUriAbsolutePath(uri: Uri): String? {
    val map = parseUriPathPart(uri)
    val basePath = parseBasePath(map, DOCUMENT_PROVIDER_PATH_DOCUMENT) ?: return null
    val storageVolumeId = parseStorageVolumeId(map, DOCUMENT_PROVIDER_PATH_DOCUMENT) ?: return null
    return buildAbsolutePath(storageVolumeId, basePath)
}

/**
 * @param uri system Child Document Uri (`content://com.android.externalstorage.documents/tree/<StorageVolume>:<BasePath>#...`)
 * @return absolute path
 * @see documentTreeUriAbsolutePath
 * @see documentUriAbsolutePath
 * @see childDocumentUriAbsolutePath
 */
internal fun documentTreeUriAbsolutePath(uri: Uri): String? {
    val map = parseUriPathPart(uri)
    val basePath = parseBasePath(map, DOCUMENT_PROVIDER_PATH_TREE) ?: return null
    val storageVolumeId = parseStorageVolumeId(map, DOCUMENT_PROVIDER_PATH_TREE) ?: return null
    return buildAbsolutePath(storageVolumeId, basePath)
}

/**
 * @param uri system Child Document Uri (`content://com.android.externalstorage.documents/tree/<StorageVolume>:<BasePath>/document/<StorageVolume>:<BasePath>#...`)
 * @return absolute path
 * @see documentTreeUriAbsolutePath
 * @see documentUriAbsolutePath
 * @see childDocumentUriAbsolutePath
 */
private fun childDocumentUriAbsolutePath(uri: Uri): String? {
    val map = parseUriPathPart(uri)
    val treeBasePath = parseBasePath(map, DOCUMENT_PROVIDER_PATH_TREE)
    val documentBasePath = parseBasePath(map, DOCUMENT_PROVIDER_PATH_TREE)
    val storageVolumeId =
        parseStorageVolumeId(map, DOCUMENT_PROVIDER_PATH_TREE)
            ?: parseStorageVolumeId(map, DOCUMENT_PROVIDER_PATH_DOCUMENT) ?: return null
    return buildAbsolutePath(storageVolumeId, documentBasePath ?: treeBasePath ?: "")
}


private fun parseUriPathPart(uri: Uri): Map<String, String> {
    val pathSegments = uri.pathSegments
    if (pathSegments.size % 2 == 0) {
        val groups = pathSegments.chunked(2)
        return groups.associate { it[0] to it[1] }
    } else {
        throw IllegalArgumentException("Unsupported uri: $uri")
    }
}

private fun parseBasePath(resolvedPath: Map<String, String>, key: String): String? {
    return resolvedPath.getOrDefault(key, "")
        .substringAfter(':', "")
        .takeIf { it.isNotEmpty() }
}

private fun parseStorageVolumeId(resolvedPath: Map<String, String>, key: String): String? {
    return resolvedPath.getOrDefault(key, "")
        .substringBefore(':', "")
        .takeIf { it.isNotEmpty() }
}

internal fun buildAbsolutePath(storageVolumeId: String, basePath: String): String {
    return if (storageVolumeId == PRIMARY) {
        externalStoragePath + basePath
    } else {
        "/storage/$storageVolumeId/$basePath"
    }
}

internal fun parseStorageVolumeId(uri: Uri): String? {
    if (!uri.isDocumentProviderUri()) return null
    val map = parseUriPathPart(uri)
    return parseStorageVolumeId(map, DOCUMENT_PROVIDER_PATH_DOCUMENT)
        ?: parseStorageVolumeId(map, DOCUMENT_PROVIDER_PATH_TREE)
}