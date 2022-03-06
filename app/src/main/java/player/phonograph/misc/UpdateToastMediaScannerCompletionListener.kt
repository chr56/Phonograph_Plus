package player.phonograph.misc

import android.annotation.SuppressLint
import android.app.Activity
import android.media.MediaScannerConnection.OnScanCompletedListener
import android.net.Uri
import android.widget.Toast
import player.phonograph.R
import java.lang.ref.WeakReference

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class UpdateToastMediaScannerCompletionListener(activity: Activity, private val toBeScanned: Array<String>) :
    OnScanCompletedListener {

    private var scanned = 0
    private var failed = 0

    private val activityWeakReference: WeakReference<Activity> = WeakReference(activity)
    private val toast = Toast.makeText(activity.applicationContext, "", Toast.LENGTH_SHORT)

    // Strings
    private val scannedFiles = activity.getString(R.string.scanned_files)
    private val couldNotScanFiles = activity.getString(R.string.could_not_scan_files)

    override fun onScanCompleted(path: String, uri: Uri?) {
        activityWeakReference.get() // activity
            ?.runOnUiThread {
                if (uri == null) { failed++ } else { scanned++ }

                val text =
                    " ${String.format(scannedFiles, scanned, toBeScanned.size)} " +
                        if (failed > 0) String.format(couldNotScanFiles, failed) else ""

                toast.setText(text)
                toast.show()
            }
    }
}
