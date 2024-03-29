package player.phonograph.ui.dialogs

import player.phonograph.R
import player.phonograph.mechanism.scanner.FileScanner
import player.phonograph.mechanism.scanner.MediaStoreScanner
import player.phonograph.model.DirectoryInfo
import player.phonograph.model.file.Location
import player.phonograph.util.coroutineToast
import player.phonograph.util.reportError
import android.os.Bundle
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ScanMediaFolderDialog : FileChooserDialog() {

    private var mediaStoreScanner: MediaStoreScanner? = null

    override fun affirmative(view: View, currentLocation: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val paths = FileScanner.listPaths(
                    DirectoryInfo(File(currentLocation.absolutePath), FileScanner.audioFileFilter)
                )
                if (!paths.isNullOrEmpty()) {
                    mediaStoreScanner?.scan(paths)
                } else {
                    coroutineToast(requireContext().applicationContext, R.string.nothing_to_scan)
                }
            } catch (e: Exception) {
                reportError(e, "ScanMediaFolderDialog", getString(R.string.failed))
            }
        }
        // dismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mediaStoreScanner = MediaStoreScanner(requireContext())
    }

    override fun onDestroyView() {
        mediaStoreScanner = null
        super.onDestroyView()
    }

    companion object {}
}
