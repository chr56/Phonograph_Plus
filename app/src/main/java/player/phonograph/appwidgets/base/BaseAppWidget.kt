package player.phonograph.appwidgets.base

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import mt.util.color.primaryTextColor
import player.phonograph.MusicServiceMsgConst
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.infoString
import player.phonograph.service.MusicService
import player.phonograph.util.ImageUtil
import player.phonograph.util.theme.getTintedDrawable

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
            if (MusicServiceMsgConst.META_CHANGED == what || MusicServiceMsgConst.PLAY_STATE_CHANGED == what) {
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

    protected fun pushUpdate(context: Context, appWidgetIds: IntArray?, views: RemoteViews) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetIds != null) {
            appWidgetManager.updateAppWidget(appWidgetIds, views)
        } else {
            appWidgetManager.updateAppWidget(ComponentName(context, javaClass), views)
        }
    }

    private fun buildPendingIntent(context: Context, action: String, serviceName: ComponentName): PendingIntent {
        val intent = Intent(action).apply { component = serviceName }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }

    protected abstract fun defaultAppWidget(context: Context, appWidgetIds: IntArray)
    abstract fun performUpdate(service: MusicService, appWidgetIds: IntArray?)

    fun setupDefaultPhonographWidgetAppearance(context: Context, view: RemoteViews) {

        view.setViewVisibility(R.id.media_titles, View.INVISIBLE)
        view.setImageViewResource(R.id.image, R.drawable.default_album_art)
        val textColor = context.primaryTextColor(true)

        view.setImageViewBitmap(
            R.id.button_next,
            ImageUtil.createBitmap(
                context.getTintedDrawable(R.drawable.ic_skip_next_white_24dp,textColor)!!
            )
        )
        view.setImageViewBitmap(
            R.id.button_prev,
            ImageUtil.createBitmap(
                context.getTintedDrawable(R.drawable.ic_skip_previous_white_24dp,textColor)!!
            )
        )
        view.setImageViewBitmap(
            R.id.button_toggle_play_pause,
            ImageUtil.createBitmap(
                context.getTintedDrawable(R.drawable.ic_play_arrow_white_24dp,textColor)!!
            )
        )
    }
    fun setupDefaultPhonographWidgetButtons(context: Context, view: RemoteViews) {
        var pendingIntent: PendingIntent? = null
        val serviceName = ComponentName(context, MusicService::class.java)

        // Previous track
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_REWIND, serviceName)
        view.setOnClickPendingIntent(R.id.button_prev, pendingIntent)

        // Play and pause
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_TOGGLE_PAUSE, serviceName)
        view.setOnClickPendingIntent(R.id.button_toggle_play_pause, pendingIntent)

        // Next track
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_SKIP, serviceName)
        view.setOnClickPendingIntent(R.id.button_next, pendingIntent)
    }

    open fun setupAdditionalWidgetAppearance(context: Context, view: RemoteViews) {}
    open fun setupAdditionalWidgetButtons(context: Context, view: RemoteViews) {}

    protected fun getAlbumArtDrawable(resources: Resources?, bitmap: Bitmap?) =
        bitmap?.let { BitmapDrawable(resources, it) }
            ?: ResourcesCompat.getDrawable(resources!!, R.drawable.default_album_art, null)

    protected fun getSongArtistAndAlbum(song: Song): String = song.infoString()

    companion object {
        const val NAME = "app_widget"
    }
}
