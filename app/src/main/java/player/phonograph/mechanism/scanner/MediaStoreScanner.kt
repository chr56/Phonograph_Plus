/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.scanner

import player.phonograph.R
import player.phonograph.foundation.notification.BackgroundNotification
import player.phonograph.util.debug
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

class MediaStoreScanner(val context: Context) : MediaScannerConnection.MediaScannerConnectionClient {

    private val scannerConnection = MediaScannerConnection(context, this)

    class Task(
        val target: Array<String>,
        var failed: MutableList<String> = mutableListOf(),
        var succeed: Int = 0,
    ) {
        val id = System.currentTimeMillis().mod(1 shl 12)
    }

    private val queue: ArrayDeque<Task> = ArrayDeque(0)

    fun scan(path: String) = scan(arrayOf(path))

    fun scan(paths: Array<String>) {
        val task = Task(paths)
        synchronized(scannerConnection) {
            if (!scannerConnection.isConnected) {
                // initial start
                queue.addFirst(task)
                scannerConnection.connect()
            } else {
                if (queue.isEmpty()) {
                    // active
                    queue.addFirst(task)
                    executeTask(queue.first())
                } else {
                    // queue
                    queue.addLast(task)
                }
            }
        }
    }

    override fun onMediaScannerConnected() {
        Log.i(TAG, "MediaScannerConnected!")

        val task = queue.firstOrNull() ?: return
        executeTask(task)
    }

    private fun executeTask(task: Task) {

        val paths: Array<String> = task.target

        Log.i(TAG, "Start scan task (${task.id})")
        reportProcess(0, paths, task.id)
        for ((index, path) in paths.withIndex()) {
            scannerConnection.scanFile(path, null)
            if (index % 17 == 0) reportProcess(index, paths, task.id)
        }
        BackgroundNotification.remove(task.id)
    }


    override fun onScanCompleted(path: String?, uri: Uri?) {
        debug {
            Log.i(TAG, "Scanned $path --> $uri")
        }
        if (path != null) {
            val task = queue.first()
            // collect
            if (uri == null) { // failed
                task.failed.add(path)
            } else {
                task.succeed++
            }
            // check if current task is completed
            if (task.succeed >= task.target.size) {
                synchronized(queue) {
                    // remove and report
                    val completed = queue.removeFirst()
                    reportResult(completed)
                    Log.i(TAG, "Scan completed for task (${completed.id})!")
                    // execute next task
                    val next = queue.firstOrNull() ?: return
                    executeTask(next)
                }
            }

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

    private fun reportResult(task: Task) {
        Handler(Looper.getMainLooper()).post {
            val failed = task.failed.size
            val succeed = task.succeed
            if (failed > 0) {
                Toast.makeText(context, String.format(couldNotScanFiles, failed), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, String.format(scannedFiles, succeed, succeed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val scannedFiles = context.getString(R.string.scanned_files)
    private val couldNotScanFiles = context.getString(R.string.could_not_scan_files)


    private val title = context.getString(R.string.background_notification_name)

    companion object {
        private const val TAG = "MediaScanner"
    }
}