package player.phonograph.util

import player.phonograph.App
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.notification.ErrorNotification
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object Util {

    fun sentPlaylistChangedLocalBoardCast() =
        LocalBroadcastManager.getInstance(App.instance).sendBroadcast(
            Intent(BROADCAST_PLAYLISTS_CHANGED)
        )

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
                val inputMethodManager =
                    activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
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

    /**
     * wrap with looper check
     */
    inline fun withLooper(crossinline block: () -> Unit) {
        if (Looper.myLooper() == null) {
            Looper.prepare()
            block()
            Looper.loop()
        } else {
            block()
        }
    }

    fun reportError(e: Throwable, tag: String, message: String) {
        Log.e(tag, message, e)
        ErrorNotification.postErrorNotification(e, message)
    }

    fun warning(tag: String, message: String) {
        Log.w(tag, message)
        ErrorNotification.postErrorNotification(message)
    }

    fun Int.testBit(mask: Int): Boolean = (this and mask) != 0
    fun Int.setBit(mask: Int): Int = (this or mask)
    fun Int.unsetBit(mask: Int): Int = (this and mask.inv())
}
