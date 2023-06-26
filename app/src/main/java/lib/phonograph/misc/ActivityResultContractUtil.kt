/*
 * Copyright (c) 2022~2023 chr_56
 */

package lib.phonograph.misc

import lib.phonograph.uri.guessDocumentUri
import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File

/**
 * Util for [ActivityResultContractTool]
 */
object ActivityResultContractUtil {

    /**
     * choose a file from Storage Access Framework
     *
     * __[context] must be [IOpenFileStorageAccess]__
     *
     * @param path initial location from guessing
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun chooseFileViaSAF(
        context: Context,
        path: String,
        mimeTypes: Array<String> = arrayOf("*/*"),
    ): Uri {
        require(context is IOpenFileStorageAccess)
        return suspendCancellableCoroutine {
            val initialUri = guessDocumentUri(context, File(path))
            context.openFileStorageAccessTool.launch(OpenDocumentContract.Config(mimeTypes, initialUri)) { uri ->
                if (uri != null) {
                    it.resume(uri, this::canceled)
                } else {
                    Log.i(TAG, "No file selected via SAF!")
                }
            }
        }
    }


    /**
     * choose a directory from Storage Access Framework
     *
     * __[context] must be [IOpenDirStorageAccess]__
     *
     * @param path initial location from guessing
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun chooseDirViaSAF(
        context: Context,
        path: String,
    ): Uri {
        require(context is IOpenDirStorageAccess)
        return suspendCancellableCoroutine {
            val initialUri = guessDocumentUri(context, File(path))
            context.openDirStorageAccessTool.launch(initialUri) { uri ->
                if (uri != null) {
                    it.resume(uri, this::canceled)
                } else {
                    Log.i(TAG, "No directory selected via SAF!")
                }
            }
        }
    }

    /**
     * create a file from Storage Access Framework
     *
     * __[context] must be [ICreateFileStorageAccess]__
     *
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun createFileViaSAF(
        context: Context,
        fileName: String,
    ): Uri {
        require(context is ICreateFileStorageAccess)
        return suspendCancellableCoroutine {
            context.createFileStorageAccessTool.launch(fileName) { uri ->
                if (uri != null) {
                    it.resume(uri, this::canceled)
                } else {
                    Log.i(TAG, "No file created via SAF!")
                }
            }
        }
    }

    private fun canceled(e: Throwable) {
        Log.i(TAG, "Canceled!")
        Log.v(TAG, "${e.message}\n${e.stackTraceToString()}")
    }

    private const val TAG = "ActivityResultContract"
}