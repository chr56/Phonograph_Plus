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
    paths: Array<String>
) :
    OnScanCompletedListener {

    private val activityWeakReference: WeakReference<Activity> = WeakReference(activity)
    private val toast = Toast.makeText(activity.applicationContext, "", Toast.LENGTH_SHORT)

    private val notificationId = paths.hashCode()
    // Strings
    private val scannedFiles = activity.getString(R.string.scanned_files)
    private val couldNotScanFiles = activity.getString(R.string.could_not_scan_files)

    private val title = activity.getString(R.string.background_notification_name)

    val total: Int = paths.size
    val rest = paths.toMutableList()
    var fail = ArrayList<String>()
    override fun onScanCompleted(path: String, uri: Uri?) {
        rest.remove(path)
        if (uri == null) fail.add(path)
        checkAndToast()
    }

    private fun checkAndToast() {
        BackgroundNotification.post(title, text, notificationId, total - rest.size, total)
        if (rest.size == 0) {
            BackgroundNotification.remove(notificationId)
            if (fail.size > 0) {
                ErrorNotification.postErrorNotification("Couldn't scan:\n${fail.reduce { acc, s -> "$acc\n$s" }}")
            }
            activityWeakReference.get()?.runOnUiThread {
                toast.setText("$text $textFail")
                toast.show()
            }
        }
    }
    private val text: String get() = " ${String.format(scannedFiles, total - fail.size, total)} "
    private val textFail: String get() = if (fail.size > 0) String.format(couldNotScanFiles, fail.size) else ""
}
