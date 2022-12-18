/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.permissions

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

typealias RequestCallback = (Map<String, Boolean>) -> Unit

private typealias Request = Pair<Array<String>, RequestCallback>

internal class PermissionDelegate {
    private lateinit var resultLauncher: ActivityResultLauncher<Array<String>>
    private var callback: RequestCallback? = null

    /**
     * call this
     */
    fun attach(activity: FragmentActivity) {
        resultLauncher =
            activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
                ::onGrantResult
            )
    }

    private fun onGrantResult(result: Map<String, Boolean>) {
        callback?.invoke(result)
        callback = null
    }

    /**
     * launch the ActivityResultLauncher
     */
    private fun start(permissions: Array<String>, callback: RequestCallback) {
        this.callback = callback
        resultLauncher.launch(permissions)
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private val queue: ArrayDeque<Request> = ArrayDeque(1)

    private fun process() {
        scope.launch {
            while (callback != null) yield() // already occupied!
            val request = queue.removeLastOrNull()
            if (request != null) {
                start(request.first, request.second)
                process()
            }
        }
    }

    fun grant(permissions: Array<String>, callback: RequestCallback) {
        queue.addFirst(permissions to callback)
        process()
    }
}