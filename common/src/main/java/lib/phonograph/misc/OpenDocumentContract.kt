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

@TargetApi(19)
class OpenDocumentContract : ActivityResultContract<OpenDocumentContract.Cfg, Uri?>() {
    override fun createIntent(context: Context, input: Cfg): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, input.mime_types)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, input.allowMultiSelect)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && input.initial_uri != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, input.initial_uri)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        }
    }
    override fun getSynchronousResult(context: Context, input: Cfg): SynchronousResult<Uri?>? = null
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
    }

    @Suppress("ArrayInDataClass")
    data class Cfg(
        val initial_uri: Uri?,
        val mime_types: Array<String>,
        val allowMultiSelect: Boolean = false
    )
}