/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.misc

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
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