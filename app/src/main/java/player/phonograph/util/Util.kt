package player.phonograph.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import player.phonograph.App
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import java.util.*

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object Util {

    @JvmStatic
    fun getStyleId(context: Context): Int {
        val outValue = TypedValue()
        val result: Boolean = context.theme.resolveAttribute(android.R.attr.theme, outValue, true)
        return if (result) outValue.resourceId else 0
    }

    fun sentPlaylistChangedLocalBoardCast() =
        LocalBroadcastManager.getInstance(App.instance).sendBroadcast(
            Intent(BROADCAST_PLAYLISTS_CHANGED)
        )

    fun navigateToStorageSetting(context: Context) {
        val uri = Uri.parse("package:${context.packageName}")
        val intent = Intent()
        intent.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                data = uri
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = uri
            }
        }
        try {
            context.startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "${e.message?.take(48)}", Toast.LENGTH_SHORT).show()
            context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS))
        }
    }

    fun currentDate(): Date = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).time
    fun currentTimestamp(): Long = currentDate().time

    fun currentDateTime(): CharSequence = DateFormat.format("yyMMdd_HHmmss", currentDate())

    fun Boolean.assertIfFalse(throwable: Throwable) {
        if (!this) throw throwable
    }

    fun Boolean.assertIfTrue(throwable: Throwable) {
        if (this) throw throwable
    }

    @JvmStatic
    fun getActionBarSize(context: Context): Int {
        val typedValue = TypedValue()
        val textSizeAttr = intArrayOf(R.attr.actionBarSize)
        val indexOfAttrTextSize = 0
        val a = context.obtainStyledAttributes(typedValue.data, textSizeAttr)
        val actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1)
        a.recycle()
        return actionBarSize
    }

    fun Context.getScreenSize(): Point {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size: Point =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager.currentWindowMetrics.bounds.run { Point(width(), height()) }
            } else {
                Point().also { windowManager.defaultDisplay.getSize(it) }
            }
        return size
    }

    @JvmStatic
    fun hideSoftKeyboard(activity: Activity?) {
        if (activity != null) {
            val currentFocus = activity.currentFocus
            if (currentFocus != null) {
                val inputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            }
        }
    }

    fun isTablet(resources: Resources): Boolean {
        return resources.configuration.smallestScreenWidthDp >= 600
    }

    @JvmStatic
    fun isLandscape(resources: Resources): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * only run [block] on [DEBUG] build
     */
     inline fun debug(crossinline block: () -> Unit) {
        if (DEBUG) block()
    }
}
