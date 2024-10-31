package player.phonograph.appwidgets

import coil.Coil
import coil.request.Disposable
import coil.request.ImageRequest
import coil.target.Target
import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.infoString
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicService
import player.phonograph.service.queue.QueueManager
import player.phonograph.ui.activities.LauncherActivity
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.ui.BitmapUtil
import util.theme.color.primaryTextColor
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
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.widget.RemoteViews

abstract class BaseAppWidget : AppWidgetProvider() {

    protected abstract val layoutId: Int

    protected abstract val name: String

    protected abstract val darkBackground: Boolean

    protected abstract val clickableAreas: IntArray

    protected abstract fun updateText(
        context: Context,
        view: RemoteViews,
        song: Song,
    )

    protected abstract fun startUpdateCover(
        context: Context,
        appWidgetIds: IntArray?,
        view: RemoteViews,
        song: Song,
        isPlaying: Boolean,
    )

    protected abstract fun updateImage(
        context: Context,
        view: RemoteViews,
        bitmap: Bitmap?,
    )


    /**
     * Update App Widget
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
     * @see android.appwidget.AppWidgetProvider.onUpdate
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        update(context, appWidgetIds, MusicPlayerRemote.isPlaying, queueManager.currentSong)
    }

    /**
     * Update widget
     */
    fun update(
        context: Context, appWidgetIds: IntArray?,
        isPlaying: Boolean, song: Song? = null,
    ) {
        drawAndPush(
            context = context,
            appWidgetIds = appWidgetIds,
            song = song ?: queueManager.currentSong,
            isPlaying = isPlaying,
            push = true,
            withCover = true
        )
    }

    private fun drawAndPush(
        context: Context, appWidgetIds: IntArray?,
        song: Song, isPlaying: Boolean,
        push: Boolean = true, withCover: Boolean = true,
    ): RemoteViews =
        buildRemoteViews(context, isPlaying, song).also { remoteViews ->
            if (cachedCover != null) {
                updateImage(context, remoteViews, cachedCover)
            } else {
                remoteViews.setImageViewResource(R.id.image, R.drawable.default_album_art)
            }
            if (push) pushUpdate(context, appWidgetIds, remoteViews)
            if (withCover) startUpdateCover(context, appWidgetIds, remoteViews, song, isPlaying)
        }


    private fun buildRemoteViews(
        context: Context,
        isPlaying: Boolean,
        song: Song,
    ): RemoteViews = RemoteViews(context.packageName, layoutId).apply {
        updateSong(context, song, isPlaying)
        setupButtonsClick(context)
        setupLaunchingClick(context)
    }

    private fun RemoteViews.updateSong(
        context: Context,
        song: Song, isPlaying: Boolean,
    ) {
        updateText(context, this, song)
        val color = context.primaryTextColor(darkBackground)
        bindDrawable(context, R.id.button_next, R.drawable.ic_skip_next_white_24dp, color)
        bindDrawable(context, R.id.button_prev, R.drawable.ic_skip_previous_white_24dp, color)
        bindDrawable(context, R.id.button_toggle_play_pause, playPauseRes(isPlaying), color)
    }

    protected fun RemoteViews.bindDrawable(
        context: Context,
        @IdRes id: Int,
        @DrawableRes drawable: Int,
        @ColorInt textColor: Int,
    ) = setImageViewBitmap(
        id, BitmapUtil.createBitmap(context.getTintedDrawable(drawable, textColor)!!)
    )

    private fun RemoteViews.setupButtonsClick(context: Context) {
        var pendingIntent: PendingIntent?
        val serviceName = ComponentName(context, MusicService::class.java)

        // Previous track
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_PREVIOUS, serviceName)
        setOnClickPendingIntent(R.id.button_prev, pendingIntent)

        // Play and pause
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_TOGGLE_PAUSE, serviceName)
        setOnClickPendingIntent(R.id.button_toggle_play_pause, pendingIntent)

        // Next track
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_NEXT, serviceName)
        setOnClickPendingIntent(R.id.button_next, pendingIntent)
    }

    private fun RemoteViews.setupLaunchingClick(context: Context) {
        for (id in clickableAreas) {
            setOnClickPendingIntent(id, clickingPendingIntent(context))
        }
    }

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
    private fun clickingPendingIntent(context: Context): PendingIntent =
        if (MusicPlayerRemote.musicService != null)
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

    protected fun getAlbumArtDrawable(resources: Resources, bitmap: Bitmap?): Drawable? =
        if (bitmap != null) {
            BitmapDrawable(resources, bitmap)
        } else {
            ResourcesCompat.getDrawable(resources, R.drawable.default_album_art, null)
        }

    protected fun playPauseRes(isPlaying: Boolean): Int =
        if (isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp


    protected fun getSongArtistAndAlbum(song: Song): String = song.infoString()


    protected var cachedCover: Bitmap? = null

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

    protected val queueManager: QueueManager get() = GlobalContext.get().get()

}
