/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.misc

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract

@TargetApi(21)
class GrandDirContract : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && input != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, input)
            }
        }
    }

    override fun getSynchronousResult(context: Context, input: Uri?): SynchronousResult<Uri?>? = null
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
}

@TargetApi(19)
class OpenDocumentContract : ActivityResultContract<OpenDocumentContract.Config, Uri?>() {
    override fun createIntent(context: Context, input: Config): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, input.mime_types)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, input.allowMultiSelect)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && input.initialUri != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, input.initialUri)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        }
    }

    override fun getSynchronousResult(context: Context, input: Config): SynchronousResult<Uri?>? = null
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
    }

    class Config(
        val mime_types: Array<String>,
        val initialUri: Uri? = null,
        val allowMultiSelect: Boolean = false,
    )
}