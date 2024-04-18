/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.file

import lib.activityresultcontract.ActivityResultContractUtil.chooseDirViaSAF
import lib.activityresultcontract.ActivityResultContractUtil.chooseFileViaSAF
import lib.storage.externalFileBashPath
import lib.storage.getAbsolutePath
import lib.storage.getBasePath
import lib.storage.getStorageId
import player.phonograph.R
import player.phonograph.util.coroutineToast
import player.phonograph.util.debug
import player.phonograph.util.warning
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import java.io.File

/**
 * select document content Uri of [filePath] via SAF
 * @param filePath absolute POSIX path of target file
 * @return content uri (`content://<EXTERNAL_STORAGE_AUTHORITY>/...`)
 */
suspend fun selectContentUri(
    context: Context,
    filePath: String,
    mimeTypes: Array<String> = arrayOf("*/*"),
): Uri? {
    val bashPath = externalFileBashPath(filePath)
    val doNotUseTree = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val segments = bashPath.split(File.separatorChar)
        when {
            segments.size == 1                                                                     -> true
            segments.size == 2 && segments[0] == Environment.DIRECTORY_DOWNLOADS                   -> true
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && segments.size >= 2
                    && segments[0] == "Android" && (segments[1] == "data" || segments[1] == "obb") -> true

            else                                                                                   -> false
        }
    } else {
        false
    }
    return if (doNotUseTree) {
        selectContentUriViaDocument(context, filePath, mimeTypes)
    } else {
        selectContentUriViaDocumentTree(context, filePath)
    }
}

/**
 * select document content Uri of [filePath] via SAF using Intent.ACTION_OPEN_DOCUMENT_TREE
 * @param filePath absolute POSIX path of target file
 * @return content uri (`content://<EXTERNAL_STORAGE_AUTHORITY>/...`)
 */
private suspend fun selectContentUriViaDocumentTree(
    context: Context,
    filePath: String,
): Uri? {
    val treeUri = chooseDirViaSAF(context, filePath)
    val documentId = run {
        val file = File(filePath)
        val storageId = file.getStorageId(context)
        val basePath = file.getBasePath()
        "$storageId:$basePath"
    }
    val childUri: Uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    val segments = childUri.pathSegments
    debug { Log.i(TAG, "Access ChildUri: $childUri") }
    return if (segments.size >= 4 && segments[3].startsWith(segments[1])) {
        childUri
    } else {
        notifyErrorChildDocumentUri(context, childUri)
        null
    }
}

/**
 * select document content Uri of [filePath] via SAF using Intent.ACTION_OPEN_DOCUMENT
 * @param filePath absolute POSIX path of target file
 * @param mimeTypes desired MIME Types
 * @return content uri (`content://<EXTERNAL_STORAGE_AUTHORITY>/...`)
 */
private suspend fun selectContentUriViaDocument(
    context: Context,
    filePath: String,
    mimeTypes: Array<String>,
): Uri? {
    val documentUri = chooseFileViaSAF(context, filePath, mimeTypes)
    if (documentUri.getAbsolutePath(context) != filePath) {
        notifyErrorDocumentUri(context, filePath, documentUri)
        return null
    }
    return documentUri
}

private suspend fun notifyErrorDocumentUri(context: Context, filePath: String, documentUri: Uri) {
    val actualPath = documentUri.getAbsolutePath(context)
    val message = buildString {
        append(context.getString(R.string.file_incorrect)).append('\n')
        append("Target:$filePath\n")
        append("Actual:$actualPath\n")
    }
    coroutineToast(context, context.getString(R.string.file_incorrect))
    warning(TAG, message)
}

private suspend fun notifyErrorChildDocumentUri(context: Context, childDocumentUri: Uri) {
    val message = buildString {
        append(context.getString(R.string.file_incorrect)).append('\n')
        val segments: List<String> = childDocumentUri.pathSegments
        if (segments.size == 4) {
            append("File is out of reach:\n")
            append("Document: ${segments[3]}\n")
            append("Tree    : ${segments[1]}\n")
        } else {
            append("File is out of reach: $childDocumentUri\n")
        }
    }
    coroutineToast(context, context.getString(R.string.file_incorrect))
    warning(TAG, message)
}

private const val TAG = "UriUtil"