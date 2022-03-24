package player.phonograph.util

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        return if (result)outValue.resourceId else 0
    }

    suspend fun coroutineToast(context: Context, text: String, longToast: Boolean = false) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context, text,
                if (longToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
        }
    }
    suspend fun coroutineToast(context: Context, @StringRes res: Int) = coroutineToast(context, context.getString(res))

    fun sentPlaylistChangedLocalBoardCast() =
        LocalBroadcastManager.getInstance(App.instance).sendBroadcast(Intent(BROADCAST_PLAYLISTS_CHANGED))

    fun currentDate(): Date = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault()).time
    fun currentTimestamp(): Long = currentDate().time

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

    @JvmStatic
    fun getScreenSize(c: Context): Point {
        val display = (c.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size = Point()
        display.getSize(size)
        return size
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun setStatusBarTranslucent(window: Window) = window.setFlags(
        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
    )

    fun setAllowDrawUnderStatusBar(window: Window) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            window.setDecorFitsSystemWindows(false)
//        } else
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
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
}
