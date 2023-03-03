/*
 * Copyright (c) 2022~2023 chr_56
 */

package lib.phonograph.misc

import lib.phonograph.storage.accessor.guessDocumentUri
import android.content.Context
import android.net.Uri
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
     * @param file initial location from guessing
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun chooseFileViaSAF(
        context: Context,
        file: File,
        mimeTypes: Array<String> = arrayOf("*/*"),
    ): Uri {
        require(context is IOpenFileStorageAccess)
        return suspendCancellableCoroutine {
            val initialUri = guessDocumentUri(context, file)
            context.openFileStorageAccessTool.launch(OpenDocumentContract.Config(mimeTypes, initialUri)) { uri ->
                if (uri != null) {
                    it.resume(uri) {}
                }
            }
        }
    }


    /**
     * choose a directory from Storage Access Framework
     *
     * __[context] must be [IOpenDirStorageAccess]__
     *
     * @param file initial location from guessing
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun chooseDirViaSAF(
        context: Context,
        file: File,
    ): Uri {
        require(context is IOpenDirStorageAccess)
        return suspendCancellableCoroutine {
            val initialUri = guessDocumentUri(context, file)
            context.openDirStorageAccessTool.launch(initialUri) { uri ->
                if (uri != null) {
                    it.resume(uri) {}
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
                    it.resume(uri) {}
                }
            }
        }
    }

}