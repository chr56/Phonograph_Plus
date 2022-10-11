package player.phonograph.ui.dialogs

import android.app.Activity
import android.media.MediaScannerConnection
import android.util.Log
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import player.phonograph.App
import player.phonograph.R
import player.phonograph.misc.UpdateToastMediaScannerCompletionListener
import player.phonograph.model.file.Location
import player.phonograph.notification.ErrorNotification
import player.phonograph.util.CoroutineUtil.coroutineToast
import player.phonograph.util.FileUtil.DirectoryInfo
import player.phonograph.util.FileUtil.FileScanner
import java.io.File

class ScanMediaFolderDialog : FileChooserDialog() {

    override fun affirmative(view: View, currentLocation: Location) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            runCatching {
                val paths = FileScanner.listPaths(
                    DirectoryInfo(File(currentLocation.absolutePath), FileScanner.audioFileFilter),
                    this
                )
                if (!paths.isNullOrEmpty()) {
                    scanFile(activity, paths)
                } else {
                    coroutineToast(requireContext().applicationContext, R.string.nothing_to_scan)
                }
            }.apply {
                if (isFailure) {
                    exceptionOrNull()?.let { e ->
                        ErrorNotification.postErrorNotification(e, "Scan Fail")
                        Log.w("ScanMediaFolderDialog", e)
                    }
                }
            }
        }
        // dismiss()
    }

    companion object {
        suspend fun scanFile(activity: Activity?, paths: Array<String>) {
            withContext(Dispatchers.Main) {
                MediaScannerConnection.scanFile(
                    activity?.applicationContext ?: App.instance,
                    paths,
                    arrayOf("audio/*", "application/ogg", "application/x-ogg", "application/itunes"),
                    if (activity != null) UpdateToastMediaScannerCompletionListener(activity, paths) else null
                )
            }
        }
    }
}
