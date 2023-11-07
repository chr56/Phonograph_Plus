package player.phonograph.ui.dialogs

import player.phonograph.App
import player.phonograph.R
import player.phonograph.misc.UpdateToastMediaScannerCompletionListener
import player.phonograph.model.file.Location
import player.phonograph.util.FileUtil.DirectoryInfo
import player.phonograph.util.FileUtil.FileScanner
import player.phonograph.util.coroutineToast
import player.phonograph.util.reportError
import android.app.Activity
import android.content.Context
import android.media.MediaScannerConnection
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ScanMediaFolderDialog : FileChooserDialog() {

    override fun affirmative(view: View, currentLocation: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val paths = FileScanner.listPaths(
                    DirectoryInfo(File(currentLocation.absolutePath), FileScanner.audioFileFilter),
                    this
                )
                if (!paths.isNullOrEmpty()) {
                    scanFile(activity ?: requireContext(), paths)
                } else {
                    coroutineToast(requireContext().applicationContext, R.string.nothing_to_scan)
                }
            } catch (e: Exception) {
                reportError(e, "ScanMediaFolderDialog", getString(R.string.failed))
            }
        }
        // dismiss()
    }

    companion object {
        suspend fun scanFile(context: Context, paths: Array<String>) {
            withContext(Dispatchers.Main) {
                MediaScannerConnection.scanFile(
                    context.applicationContext ?: App.instance,
                    paths,
                    arrayOf("audio/*", "application/ogg", "application/x-ogg", "application/itunes"),
                    if (context is Activity) UpdateToastMediaScannerCompletionListener(context, paths) else null
                )
            }
        }
    }
}
