/*
 * Copyright (c) 2022~2023 chr_56
 */

package lib.phonograph.misc

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import android.net.Uri

/**
 * register [ActivityResultContractTool] in onCreate using [register], then you can use everywhere
 */
abstract class ActivityResultContractTool<I, O> {

    abstract fun key(): String
    abstract fun contract(): ActivityResultContract<I, O>


    private var launcher: ActivityResultLauncher<I>? = null
    private var callback: ((O) -> Unit)? = null

    var busy: Boolean = false
        private set

    fun register(lifeCycle: Lifecycle, registry: ActivityResultRegistry) {
        lifeCycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                register(owner, registry)
            }
        })
    }

    private fun register(owner: LifecycleOwner, registry: ActivityResultRegistry) {
        launcher = registry.register(key(), owner, contract()) {
            val callback = this.callback
            if (callback != null) {
                callback.invoke(it)
            } else {
                throw IllegalStateException("callback of ActivityResult is null!")
            }
            this.callback = null
            busy = false
        }
    }

    @Synchronized
    fun launch(input: I, callback: (O) -> Unit) {
        val launcher = this.launcher
        if (launcher != null) {
            busy = true
            this.callback = callback
            launcher.launch(input)
        } else {
            throw IllegalStateException("ActivityResultLauncher is not correctly registered!")
        }
    }
}

class OpenFileStorageAccessTool : ActivityResultContractTool<OpenDocumentContract.Cfg, Uri?>() {
    override fun key(): String = "OpenFile"
    override fun contract(): ActivityResultContract<OpenDocumentContract.Cfg, Uri?> =
        OpenDocumentContract()
}

class OpenDirStorageAccessTool : ActivityResultContractTool<Uri?, Uri?>() {
    override fun key(): String = "OpenDir"
    override fun contract(): ActivityResultContract<Uri?, Uri?> =
        GrandDirContract()
}

class CreateFileStorageAccessTool(val mimeType: String = "*/*") : ActivityResultContractTool<String, Uri?>() {
    override fun key(): String = "CreateFile"
    override fun contract(): ActivityResultContract<String, Uri?> =
        ActivityResultContracts.CreateDocument(mimeType)
}

interface IOpenFileStorageAccess {
    val openFileStorageAccessTool: OpenFileStorageAccessTool
}

interface IOpenDirStorageAccess {
    val openDirStorageAccessTool: OpenDirStorageAccessTool
}

interface ICreateFileStorageAccess {
    val createFileStorageAccessTool: CreateFileStorageAccessTool
}