package player.phonograph.appwidgets.base

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.service.MusicService
import player.phonograph.util.MusicUtil

abstract class BaseAppWidget : AppWidgetProvider() {

    /**
     * @see android.appwidget.AppWidgetProvider.onUpdate
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        defaultAppWidget(context, appWidgetIds)
        val updateIntent =
            Intent(MusicService.APP_WIDGET_UPDATE).apply {
                putExtra(MusicService.EXTRA_APP_WIDGET_NAME, NAME)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
            }
        context.sendBroadcast(updateIntent)
    }

    /**
     * Handle a change notification coming over from
     * [MusicService]
     */
    fun notifyChange(service: MusicService, what: String) {
        if (hasInstances(service)) {
            if (MusicService.META_CHANGED == what || MusicService.PLAY_STATE_CHANGED == what) {
                performUpdate(service, null)
            }
        }
    }

    /**
     * Check against [AppWidgetManager] if there are any instances of this
     * widget.
     */
    protected fun hasInstances(context: Context): Boolean {
        val mAppWidgetIds =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, javaClass))
        return mAppWidgetIds.isNotEmpty()
    }

    protected fun pushUpdate(context: Context?, appWidgetIds: IntArray?, views: RemoteViews?) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetIds != null) {
            appWidgetManager.updateAppWidget(appWidgetIds, views)
        } else {
            appWidgetManager.updateAppWidget(ComponentName(context!!, javaClass), views)
        }
    }

    protected fun buildPendingIntent(context: Context?, action: String?, serviceName: ComponentName?): PendingIntent {
        val intent = Intent(action).apply { component = serviceName }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }

    protected abstract fun defaultAppWidget(context: Context, appWidgetIds: IntArray)

    abstract fun performUpdate(service: MusicService?, appWidgetIds: IntArray?)
    protected fun getAlbumArtDrawable(resources: Resources?, bitmap: Bitmap?): Drawable? {
        return bitmap?.let { BitmapDrawable(resources, it) } ?: ResourcesCompat.getDrawable(resources!!, R.drawable.default_album_art, null)
    }

    protected fun getSongArtistAndAlbum(song: Song?): String {
        return MusicUtil.getSongInfoString(song!!)
    }

    companion object {
        const val NAME = "app_widget"
    }
}
