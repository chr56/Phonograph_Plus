package player.phonograph.appwidgets.base

import coil.Coil
import coil.request.Disposable
import coil.request.ImageRequest
import coil.target.Target
import mt.util.color.primaryTextColor
import org.koin.core.context.GlobalContext
import player.phonograph.MusicServiceMsgConst
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.infoString
import player.phonograph.service.MusicService
import player.phonograph.service.queue.QueueManager
import player.phonograph.ui.activities.LauncherActivity
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.ui.BitmapUtil
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.core.content.res.ResourcesCompat
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.widget.RemoteViews

abstract class BaseAppWidget : AppWidgetProvider() {

    protected abstract val layoutId: Int

    protected abstract val name: String

    protected abstract val darkBackground: Boolean

    /**
     * @see android.appwidget.AppWidgetProvider.onUpdate
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {

        val color = context.primaryTextColor(darkBackground)

        val remoteViews = buildRemoteViews(
            context,
            isPlaying = false,
            queueManager.currentSong,
            color
        )

        remoteViews.setImageViewResource(R.id.image, R.drawable.default_album_art)

        pushUpdate(context, appWidgetIds, remoteViews)

        context.sendBroadcast(
            Intent(MusicService.APP_WIDGET_UPDATE).apply {
                putExtra(MusicService.EXTRA_APP_WIDGET_NAME, name)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
            }
        )
    }


    protected fun buildRemoteViews(
        context: Context,
        isPlaying: Boolean,
        song: Song,
        @ColorInt color: Int,
    ): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, layoutId)

        setupDefaultPhonographWidgetButtons(context, remoteViews)
        setupLaunchingClick(context, remoteViews)

        updateSong(context, remoteViews, song, isPlaying, color)

        return remoteViews
    }

    /**
     * Handle a change notification coming over from [MusicService]
     */
    fun notifyChange(context: Context, isPlaying: Boolean, what: String) {
        val hasInstances = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, javaClass)).isNotEmpty()
        if (hasInstances) { // update only having widgets
            if (MusicServiceMsgConst.META_CHANGED == what || MusicServiceMsgConst.PLAY_STATE_CHANGED == what) {
                performUpdate(context, isPlaying, null)
            }
        }
    }

    /**
     * Update App Widget with [AppWidgetManager]
     */
    protected fun pushUpdate(context: Context, appWidgetIds: IntArray?, views: RemoteViews) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetIds != null) {
            appWidgetManager.updateAppWidget(appWidgetIds, views)
        } else {
            appWidgetManager.updateAppWidget(ComponentName(context, javaClass), views)
        }
    }

    /**
     * Update all active widget instances by pushing changes
     */
    fun performUpdate(context: Context, isPlaying: Boolean, appWidgetIds: IntArray?) {
        isServiceStarted = true

        val textColor = context.primaryTextColor(darkBackground)
        val song = queueManager.currentSong

        val remoteViews = buildRemoteViews(context, isPlaying, song, textColor)

        pushUpdate(context, appWidgetIds, remoteViews)

        startUpdateCover(context, remoteViews, song, isPlaying, appWidgetIds)
    }

    abstract fun updateText(context: Context, view: RemoteViews, song: Song)

    abstract fun startUpdateCover(
        context: Context,
        view: RemoteViews,
        song: Song,
        isPlaying: Boolean,
        appWidgetIds: IntArray?,
    )

    private fun updateSong(
        context: Context, view: RemoteViews,
        song: Song, isPlaying: Boolean, @ColorInt color: Int,
    ) {
        updateText(context, view, song)
        view.bindDrawable(context, R.id.button_next, R.drawable.ic_skip_next_white_24dp, color)
        view.bindDrawable(context, R.id.button_prev, R.drawable.ic_skip_previous_white_24dp, color)
        view.bindDrawable(context, R.id.button_toggle_play_pause, playPauseRes(isPlaying), color)
    }

    protected fun RemoteViews.bindDrawable(
        context: Context,
        @IdRes id: Int,
        @DrawableRes drawable: Int,
        @ColorInt textColor: Int,
    ) = setImageViewBitmap(
        id, BitmapUtil.createBitmap(context.getTintedDrawable(drawable, textColor)!!)
    )

    private fun setupDefaultPhonographWidgetButtons(context: Context, view: RemoteViews) {
        var pendingIntent: PendingIntent?
        val serviceName = ComponentName(context, MusicService::class.java)

        // Previous track
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_PREVIOUS, serviceName)
        view.setOnClickPendingIntent(R.id.button_prev, pendingIntent)

        // Play and pause
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_TOGGLE_PAUSE, serviceName)
        view.setOnClickPendingIntent(R.id.button_toggle_play_pause, pendingIntent)

        // Next track
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_NEXT, serviceName)
        view.setOnClickPendingIntent(R.id.button_next, pendingIntent)
    }

    abstract fun setupLaunchingClick(context: Context, view: RemoteViews)

    private fun buildPendingIntent(context: Context, action: String, serviceName: ComponentName): PendingIntent {
        val intent = Intent(action).apply { component = serviceName }
        return if (SDK_INT >= VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }


    /**
     * PendingIntent for launching [MusicService] or [LauncherActivity]
     */
    protected fun clickingPendingIntent(context: Context): PendingIntent =
        if (isServiceStarted)
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, LauncherActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        else
            buildPendingIntent(context, "", ComponentName(context, MusicService::class.java))

    protected fun getAlbumArtDrawable(resources: Resources?, bitmap: Bitmap?) =
        bitmap?.let { BitmapDrawable(resources, it) }
            ?: ResourcesCompat.getDrawable(resources!!, R.drawable.default_album_art, null)

    protected fun playPauseRes(isPlaying: Boolean): Int =
        if (isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp


    protected fun getSongArtistAndAlbum(song: Song): String = song.infoString()

    protected val queueManager: QueueManager get() = GlobalContext.get().get()


    private var task: Disposable? = null
    protected fun loadImage(
        context: Context,
        song: Song,
        widgetImageSize: Int,
        target: Target,
    ) {
        val loader = Coil.imageLoader(context)
        task?.dispose()
        task = loader.enqueue(
            ImageRequest.Builder(context)
                .data(song)
                .size(widgetImageSize)
                .target(target)
                .build()
        )
    }

    private var isServiceStarted: Boolean = false
}
