package player.phonograph.misc

import android.app.Activity
import android.media.MediaScannerConnection.OnScanCompletedListener
import android.net.Uri
import android.widget.Toast
import java.lang.ref.WeakReference
import player.phonograph.R
import player.phonograph.notification.BackgroundNotification
import player.phonograph.notification.ErrorNotification

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class UpdateToastMediaScannerCompletionListener(
    activity: Activity,
    paths: Array<String>,
) :
    OnScanCompletedListener {

    private val activityWeakReference: WeakReference<Activity> = WeakReference(activity)

    private val notificationId = paths.hashCode()

    // Strings
    private val scannedFiles = activity.getString(R.string.scanned_files)
    private val couldNotScanFiles = activity.getString(R.string.could_not_scan_files)

    private val title = activity.getString(R.string.background_notification_name)

    val total: Int = paths.size
    var fail = ArrayList<String>()
    var success = 0
    override fun onScanCompleted(path: String, uri: Uri?) {
        if (uri == null) fail.add(path) else success++
        checkAndToast()
    }

    private fun checkAndToast() {
        BackgroundNotification.post(title, text, notificationId, success, total)
        if (success + fail.size >= total) {
            BackgroundNotification.remove(notificationId)
            if (fail.size > 0) {
                ErrorNotification.postErrorNotification("Couldn't scan:\n${fail.reduce { acc, s -> "$acc\n$s" }}")
            }
            activityWeakReference.get()?.let { activity ->
                activity.runOnUiThread {
                    Toast.makeText(
                        activity.applicationContext,
                        "$text $textFail",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }
    }

    private val text: String get() = " ${String.format(scannedFiles,success, total)} "
    private val textFail: String get() = if (fail.size > 0) String.format(couldNotScanFiles, fail.size) else ""
}
