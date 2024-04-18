/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.file

import lib.activityresultcontract.ActivityResultContractUtil.chooseDirViaSAF
import lib.activityresultcontract.ActivityResultContractUtil.chooseFileViaSAF
import lib.storage.documentProviderUriAbsolutePath
import lib.storage.externalFileBashPath
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
 * select document content Uri from [filePaths] via SAF
 * @param filePaths absolute POSIX paths of target file
 * @return list of document content Uri (`<EXTERNAL_STORAGE_AUTHORITY>/tree/.../child/...`)
 */
suspend fun selectDocumentUris(
    context: Context,
    filePaths: List<String>,
): List<Uri> {
    val treeUri = selectDocumentTreeUri(context, filePaths) ?: return emptyList()
    return filePaths.map { filePath -> childDocumentUriWithinTree(context, treeUri, filePath) }
}

/**
 * select document tree content Uri from [filePaths] via SAF
 * @param filePaths absolute POSIX paths of target file
 * @return document tree content Uri (`content://<EXTERNAL_STORAGE_AUTHORITY>/tree/...`)
 */
suspend fun selectDocumentTreeUri(
    context: Context,
    filePaths: List<String>,
): Uri? {
    if (filePaths.isEmpty()) return null
    val commonRoot = commonPathRoot(filePaths).ifEmpty { Environment.getExternalStorageDirectory().absolutePath }
    val treeUri = chooseDirViaSAF(context, commonRoot)
    Log.v(TAG, "Select shared Document Tree Content Uri: $treeUri")
    return treeUri
}


/**
 * select document content Uri of [filePath] via SAF
 * @param filePath absolute POSIX path of target file
 * @return content uri (`content://<EXTERNAL_STORAGE_AUTHORITY>/...`);
 *          maybe a normal Document Uri or Child Document Uri inside Document Tree Uri
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
 * @return content uri (`content://<EXTERNAL_STORAGE_AUTHORITY>/tree/.../child/...`)
 */
private suspend fun selectContentUriViaDocumentTree(
    context: Context,
    filePath: String,
): Uri? {
    val treeUri = chooseDirViaSAF(context, filePath)
    val childUri: Uri = childDocumentUriWithinTree(context, treeUri, filePath)
    val segments = childUri.pathSegments
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
 * @return content uri (`content://<EXTERNAL_STORAGE_AUTHORITY>/document/...`)
 */
private suspend fun selectContentUriViaDocument(
    context: Context,
    filePath: String,
    mimeTypes: Array<String>,
): Uri? {
    val documentUri = chooseFileViaSAF(context, filePath, mimeTypes)
    if (filePath != documentProviderUriAbsolutePath(documentUri, context)) {
        notifyErrorDocumentUri(context, filePath, documentUri)
        return null
    }
    return documentUri
}

private suspend fun notifyErrorDocumentUri(context: Context, filePath: String, documentUri: Uri) {
    val actualPath = documentProviderUriAbsolutePath(documentUri, context)
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


/**
 * Use [treeUri] to build document content uri of file [filePath]
 * @param treeUri Document Tree Uri (`content://<EXTERNAL_STORAGE_AUTHORITY>/tree/...`)
 * @param filePath absolute POSIX paths of target file
 * @return document content uri (`content://<EXTERNAL_STORAGE_AUTHORITY>/tree/.../child/...`)
 */
private fun childDocumentUriWithinTree(context: Context, treeUri: Uri, filePath: String): Uri {
    val documentId = run {
        val file = File(filePath)
        val storageId = file.getStorageId(context)
        val basePath = file.getBasePath()
        "$storageId:$basePath"
    }
    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
    debug { Log.i(TAG, "Access ChildUri: ${childUri.path}") }
    return childUri
}


/**
 * common path root of a list of paths
 * @param paths list of path separating by '/'
 * @return common root path of these [paths], **empty** if no common
 */
private fun commonPathRoot(paths: Collection<String>): String {

    val fragments = paths.map { path -> path.split('/').filter { it.isNotEmpty() } }
    val result = mutableListOf<String>()

    var index = 0
    while (true) {
        val col = fragments.mapNotNull { it.getOrNull(index) }.toSet()
        if (col.size == 1) {
            result.add(col.first())
            index++
        } else {// size > 1 or size == 0
            break
        }
    }

    return result.fold("") { acc, s -> "$acc/$s" }
}

private const val TAG = "UriUtil"