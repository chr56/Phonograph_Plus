/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.misc

import player.phonograph.R
import player.phonograph.notification.BackgroundNotification
import player.phonograph.util.debug
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log

class MediaScanner(val context: Context) : MediaScannerConnection.MediaScannerConnectionClient {

    private val scannerConnection = MediaScannerConnection(context, this)

    private var target: Array<String>? = null
    private var failed: MutableList<String> = mutableListOf()
    private var successed: Int = 0


    fun scan(path: String) = scan(arrayOf(path))

    fun scan(paths: Array<String>) {
        synchronized(scannerConnection) {
            if (!scannerConnection.isConnected) {
                successed = 0
                failed.clear()
                target = paths
                scannerConnection.connect()
            } else {
                // cancel
                scannerConnection.disconnect()
                target = null
                successed = 0
                failed.clear()
            }
        }
    }

    override fun onMediaScannerConnected() {
        Log.i(TAG, "Start scanning...")

        val paths = target ?: return
        val id = paths.hashCode()
        reportProcess(0, paths, id)
        for ((index, path) in paths.withIndex()) {
            scannerConnection.scanFile(path, null)
            if (index % 17 == 0) reportProcess(index, paths, id)
        }
        BackgroundNotification.remove(id)
        // reportResult()
        // scannerConnection.disconnect()
    }


    override fun onScanCompleted(path: String?, uri: Uri?) {
        debug {
            Log.i(TAG, "Scanned $path --> $uri")
        }
        if (uri == null && path != null) { // failed
            failed.add(path)
        } else {
            successed++
        }
    }

    private fun reportProcess(current: Int, all: Array<String>, id: Int) {
        BackgroundNotification.post(
            title,
            String.format(scannedFiles, current, all.size),
            id,
            current,
            all.size
        )
    }

    /*
    private fun reportResult() {
        Handler(Looper.getMainLooper()).post {
            val failed = failed.size
            if (failed > 0) {
                Toast.makeText(context, String.format(couldNotScanFiles, failed), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, String.format(scannedFiles, successed, successed), Toast.LENGTH_SHORT).show()
            }
        }
    }
     */

    private val scannedFiles = context.getString(R.string.scanned_files)
    private val couldNotScanFiles = context.getString(R.string.could_not_scan_files)


    private val title = context.getString(R.string.background_notification_name)

    companion object {
        private const val TAG = "MediaScanner"
    }
}