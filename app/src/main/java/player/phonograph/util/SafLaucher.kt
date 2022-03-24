/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

typealias UriCallback = (Uri?) -> Any
class SafLauncher(private val registry: ActivityResultRegistry) : DefaultLifecycleObserver {
    private lateinit var createLauncher: ActivityResultLauncher<String>
    lateinit var createCallback: UriCallback
    var createCallbackInUse = false
        private set

    private lateinit var dirLauncher: ActivityResultLauncher<Uri?>
    lateinit var dirCallback: UriCallback
    var dirCallbackInUse = false
        private set

    private lateinit var openLauncher: ActivityResultLauncher<OpenDocumentContract.Cfg>
    lateinit var openCallback: UriCallback
    var openCallbackInUse = false
        private set
    override fun onCreate(owner: LifecycleOwner) {
        createLauncher = registry.register("CreateFile", owner, ActivityResultContracts.CreateDocument()) {
            createCallback(it)
            createCallbackInUse = false
        }
        dirLauncher = registry.register("OpenDir", owner, GrandDirContract()) {
            dirCallback(it)
            dirCallbackInUse = false
        }
        openLauncher = registry.register("OpenFile", owner, OpenDocumentContract()) {
            openCallback(it)
            openCallbackInUse = false
        }
    }

    fun createFile(fileName: String, callback: UriCallback) {
        if (createCallbackInUse) return // todo
        createCallbackInUse = true
        this.createCallback = callback
        createLauncher.launch(fileName)
    }
    fun openDir(dir: Uri, callback: UriCallback) {
        if (dirCallbackInUse) return // todo
        dirCallbackInUse = true
        this.dirCallback = callback
        dirLauncher.launch(dir)
    }
    fun openFile(cfg: OpenDocumentContract.Cfg, callback: UriCallback) {
        if (openCallbackInUse) return // todo
        openCallbackInUse = true
        this.openCallback = callback
        openLauncher.launch(cfg)
    }
}
interface SAFCallbackHandlerActivity {
    fun getSafLauncher(): SafLauncher
}

@TargetApi(21)
class GrandDirContract : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        return Intent(ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION or FLAG_GRANT_PERSISTABLE_URI_PERMISSION or FLAG_GRANT_PREFIX_URI_PERMISSION
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
class OpenDocumentContract : ActivityResultContract<OpenDocumentContract.Cfg, Uri?>() {
    override fun createIntent(context: Context, input: Cfg): Intent {
        return Intent(ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(EXTRA_MIME_TYPES, input.mime_types)
            putExtra(EXTRA_ALLOW_MULTIPLE, input.allowMultiSelect)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && input.initial_uri != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, input.initial_uri)
                addCategory(CATEGORY_OPENABLE)
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
