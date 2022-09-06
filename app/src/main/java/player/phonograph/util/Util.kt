package player.phonograph.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.yield
import player.phonograph.App
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED
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
     * a class to help convert callback-style function to async-coroutine-style function
     */
    class Executor<R>(val block: (Wrapper<R?>) -> Unit) {
        private var holder: Wrapper<R?> = Wrapper(null)
        suspend fun execute(): R {
            block(holder)
            while (holder.content == null) yield()
            return holder.content!!
        }

        class Wrapper<T>(var content: T?)
    }
}
