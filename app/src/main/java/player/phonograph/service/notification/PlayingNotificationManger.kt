/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.notification

import coil.request.Disposable
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.service.MusicService
import player.phonograph.service.ServiceComponent
import player.phonograph.service.player.PlayerState.PAUSED
import player.phonograph.service.player.PlayerState.PLAYING
import player.phonograph.service.player.PlayerState.PREPARING
import player.phonograph.service.player.PlayerState.STOPPED
import player.phonograph.settings.Keys
import player.phonograph.settings.PrimitiveKey
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.MainActivity
import player.phonograph.util.theme.createTintedDrawable
import player.phonograph.util.ui.BitmapUtil
import androidx.annotation.RequiresApi
import androidx.media.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service.STOP_FOREGROUND_DETACH
import android.app.Service.STOP_FOREGROUND_REMOVE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.core.app.NotificationCompat as XNotificationCompat
import android.app.Notification as OSNotification

class PlayingNotificationManger : ServiceComponent {

    private var _service: MusicService? = null
    private val service: MusicService get() = _service!!

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: XNotificationCompat.Builder

    private var classicNotification: Boolean = false
    private var coloredNotification: Boolean = true

    var persistent: Boolean = false
        private set

    private var impl: Impl? = null

    override fun onCreate(musicService: MusicService) {
        _service = musicService


        notificationManager =
            musicService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder = XNotificationCompat.Builder(musicService, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(clickPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setVisibility(XNotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setPriority(XNotificationCompat.PRIORITY_MAX)
            .setCategory(XNotificationCompat.CATEGORY_TRANSPORT)

        if (SDK_INT >= Build.VERSION_CODES.O) {
            val channel: NotificationChannel? = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            if (channel == null) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        musicService.getString(R.string.playing_notification_name),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = musicService.getString(R.string.playing_notification_description)
                        enableLights(false)
                        enableVibration(false)
                    }
                )
            }
        }

        fun <T> collect(key: PrimitiveKey<T>, collector: FlowCollector<T>) {
            service.coroutineScope.launch(SupervisorJob()) {
                Setting(musicService)[key].flow.distinctUntilChanged().collect(collector)
            }
        }
        collect(Keys.classicNotification) { value ->
            classicNotification = value
        }
        collect(Keys.coloredNotification) { value ->
            coloredNotification = value
        }
        collect(Keys.persistentPlaybackNotification) { value ->
            persistent = value
        }

        impl = if (classicNotification) Impl0() else Impl24()
    }

    override fun onDestroy(musicService: MusicService) {
        removeNotification()
        impl = null
        _service = null
    }

    fun updateNotification(song: Song) {
        if (song.id != -1L) {
            impl?.update(song)
        } else {
            if (persistent) {
                impl?.empty()
            } else {
                removeNotification()
            }
        }
    }

    private fun postNotification(notification: OSNotification) {
        when (persistent) {
            true  -> {
                notificationManager.notify(NOTIFICATION_ID, notification)
                service.startForeground(NOTIFICATION_ID, notification)
            }

            false -> {
                if (service.isDestroyed) {
                    // service stopped
                    removeNotification()
                } else {
                    notificationManager.notify(NOTIFICATION_ID, notification)
                    when (service.playerState) {
                        PLAYING, PAUSED    -> service.startForeground(NOTIFICATION_ID, notification)
                        STOPPED, PREPARING -> service.stopForeground(STOP_FOREGROUND_DETACH)
                    }
                }
            }
        }
    }

    fun cancelNotification() {
        if (!persistent) removeNotification()
    }

    @Synchronized
    private fun removeNotification() {
        service.stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    //region Impl

    internal interface Impl {
        fun update(song: Song)
        fun empty()
    }

    /** Disposable ImageRequest for Cover Art **/
    private var request: Disposable? = null

    inner class Impl24 : Impl {

        private fun playPauseAction(isPlaying: Boolean): XNotificationCompat.Action = XNotificationCompat.Action(
            if (isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp,
            service.getString(R.string.action_play_pause),
            buildPlaybackPendingIntent(MusicService.ACTION_TOGGLE_PAUSE)
        )

        private fun previousAction(): XNotificationCompat.Action = XNotificationCompat.Action(
            R.drawable.ic_skip_previous_white_24dp,
            service.getString(R.string.action_previous),
            buildPlaybackPendingIntent(MusicService.ACTION_REWIND)
        )

        private fun nextAction(): XNotificationCompat.Action = XNotificationCompat.Action(
            R.drawable.ic_skip_next_white_24dp,
            service.getString(R.string.action_next),
            buildPlaybackPendingIntent(MusicService.ACTION_SKIP)
        )

        private fun mediaStyle(): NotificationCompat.MediaStyle =
            NotificationCompat.MediaStyle().setMediaSession(service.mediaSession.sessionToken)

        @Synchronized
        override fun update(song: Song) {
            val isPlaying = service.isPlaying

            prepareNotification(
                title = song.title,
                content = song.artistName,
                subText = song.albumName,
                ongoing = isPlaying,
                style = mediaStyle().setShowActionsInCompactView(0, 1, 2),
                previousAction(), playPauseAction(isPlaying), nextAction()
            )

            postNotification(notificationBuilder.build())

            if (SDK_INT < VERSION_SET_COVER_USING_METADATA) {
                request?.dispose()
                request = service.coverLoader.load(song) { bitmap: Bitmap?, paletteColor: Int ->
                    if (bitmap != null) {
                        notificationBuilder
                            .setLargeIcon(bitmap)
                            .also { builder ->
                                if (SDK_INT <= Build.VERSION_CODES.O && coloredNotification) {
                                    builder.color = paletteColor
                                }
                            }
                        postNotification(notificationBuilder.build())
                    }
                }
            }
        }

        /**
         * prepare notification
         */
        private fun prepareNotification(
            title: String,
            content: String?,
            subText: String?,
            ongoing: Boolean,
            style: XNotificationCompat.Style,
            vararg actions: XNotificationCompat.Action,
        ): XNotificationCompat.Builder =
            notificationBuilder
                .setContentTitle(title)
                .setContentText(content)
                .setSubText(subText)
                .setOngoing(ongoing)
                .setLargeIcon(service.coverLoader.defaultCover)
                .clearActions()
                .apply {
                    for (action in actions) {
                        addAction(action)
                    }
                }
                .setStyle(style)


        private fun emptyNotification() =
            prepareNotification(
                title = service.getString(R.string.empty),
                content = null,
                subText = null,
                ongoing = true,
                style = mediaStyle(),
            )

        override fun empty() {
            postNotification(emptyNotification().build())
        }
    }

    inner class Impl0 : Impl {

        override fun empty() {
            val notificationLayout: RemoteViews =
                prepareNotificationLayout(service.getString(R.string.empty), null, null, Color.LTGRAY)

            val notificationLayoutBig: RemoteViews =
                prepareNotificationLayoutBig(service.getString(R.string.empty), null, null, Color.LTGRAY)

            updateNotificationContent(Color.LTGRAY, notificationLayout, notificationLayoutBig)

            notificationBuilder
                .setContent(notificationLayout)
                .setCustomBigContentView(notificationLayoutBig)
                .setOngoing(true)

            postNotification(notificationBuilder.build())
        }

        @Synchronized
        override fun update(song: Song) {

            val notificationLayout: RemoteViews =
                prepareNotificationLayout(song.title, song.artistName, song.albumName, Color.WHITE)

            val notificationLayoutBig: RemoteViews =
                prepareNotificationLayoutBig(song.title, song.artistName, song.albumName, Color.WHITE)

            updateNotificationContent(Color.WHITE, notificationLayout, notificationLayoutBig)

            notificationBuilder
                .setContent(notificationLayout)
                .setCustomBigContentView(notificationLayoutBig)
                .setOngoing(service.isPlaying)

            postNotification(notificationBuilder.build())

            // then try to load cover image
            request?.dispose()
            request = service.coverLoader.load(song) { bitmap: Bitmap?, backgroundColor: Int ->
                if (bitmap != null) {
                    notificationLayout.setImageViewBitmap(R.id.image, bitmap)
                    notificationLayoutBig.setImageViewBitmap(R.id.image, bitmap)
                    if (coloredNotification) {
                        notificationLayout.setBackgroundColor(backgroundColor)
                        notificationLayoutBig.setBackgroundColor(backgroundColor)
                        updateNotificationContent(backgroundColor, notificationLayout, notificationLayoutBig)
                    }
                    postNotification(notificationBuilder.build())
                }
            }
        }


        private fun prepareNotificationLayout(
            title: String?,
            text1: String?,
            @Suppress("UNUSED_PARAMETER") text2: String?,
            backgroundColor: Int,
        ): RemoteViews {
            //region Collapsed Notification
            val notificationLayout: RemoteViews =
                RemoteViews(service.packageName, R.layout.notification).apply {
                    //region Text
                    if (TextUtils.isEmpty(title) && TextUtils.isEmpty(text1)) {
                        setViewVisibility(R.id.media_titles, View.INVISIBLE)
                    } else {
                        setViewVisibility(R.id.media_titles, View.VISIBLE)
                        setTextViewText(R.id.title, title)
                        setTextViewText(R.id.text, text1)
                    }
                    //endregion
                    //region Default Artwork
                    setImageViewResource(R.id.image, R.drawable.default_album_art)
                    //endregion
                    //region Color
                    setBackgroundColor(backgroundColor)
                    //endregion
                    //region Actions
                    setupActionButtons()
                    //endregion
                }
            //endregion
            return notificationLayout
        }

        private fun prepareNotificationLayoutBig(
            title: String?,
            text1: String?,
            text2: String?,
            backgroundColor: Int,
        ): RemoteViews {
            //region Expanded Notification
            val notificationLayoutBig: RemoteViews =
                RemoteViews(service.packageName, R.layout.notification_big).apply {
                    //region Text
                    if (TextUtils.isEmpty(title) && TextUtils.isEmpty(text1) && TextUtils.isEmpty(text2)) {
                        setViewVisibility(R.id.media_titles, View.INVISIBLE)
                    } else {
                        setViewVisibility(R.id.media_titles, View.VISIBLE)
                        setTextViewText(R.id.title, title)
                        setTextViewText(R.id.text, text1)
                        setTextViewText(R.id.text2, text2)
                    }
                    //endregion
                    //region Default Artwork
                    setImageViewResource(R.id.image, R.drawable.default_album_art)
                    //endregion
                    //region Color
                    setBackgroundColor(backgroundColor)
                    //endregion
                    //region Actions
                    setupActionButtons()
                    //endregion
                }
            //endregion
            return notificationLayoutBig
        }

        private fun RemoteViews.setBackgroundColor(color: Int) =
            setInt(R.id.root, "setBackgroundColor", color)

        private fun RemoteViews.setupActionButtons() {
            @Suppress("JoinDeclarationAndAssignment")
            var pendingIntent: PendingIntent

            // Previous track
            pendingIntent = buildPlaybackPendingIntent(MusicService.ACTION_REWIND)
            setOnClickPendingIntent(R.id.action_prev, pendingIntent)

            // Play and pause
            pendingIntent = buildPlaybackPendingIntent(MusicService.ACTION_TOGGLE_PAUSE)
            setOnClickPendingIntent(R.id.action_play_pause, pendingIntent)

            // Next track
            pendingIntent = buildPlaybackPendingIntent(MusicService.ACTION_SKIP)
            setOnClickPendingIntent(R.id.action_next, pendingIntent)
        }

        private fun linkButtons(notificationLayout: RemoteViews, notificationLayoutBig: RemoteViews) {
            @Suppress("JoinDeclarationAndAssignment")
            var pendingIntent: PendingIntent

            // Previous track
            pendingIntent = buildPlaybackPendingIntent(MusicService.ACTION_REWIND)
            notificationLayout.setOnClickPendingIntent(R.id.action_prev, pendingIntent)
            notificationLayoutBig.setOnClickPendingIntent(R.id.action_prev, pendingIntent)

            // Play and pause
            pendingIntent = buildPlaybackPendingIntent(MusicService.ACTION_TOGGLE_PAUSE)
            notificationLayout.setOnClickPendingIntent(R.id.action_play_pause, pendingIntent)
            notificationLayoutBig.setOnClickPendingIntent(R.id.action_play_pause, pendingIntent)

            // Next track
            pendingIntent = buildPlaybackPendingIntent(MusicService.ACTION_SKIP)
            notificationLayout.setOnClickPendingIntent(R.id.action_next, pendingIntent)
            notificationLayoutBig.setOnClickPendingIntent(R.id.action_next, pendingIntent)
        }


        private fun updateNotificationContent(
            backgroundColor: Int,
            notificationLayout: RemoteViews,
            notificationLayoutBig: RemoteViews,
        ) {
            val primary = service.primaryTextColor(backgroundColor)
            val secondary = service.secondaryTextColor(backgroundColor)

            val prev = BitmapUtil.createBitmap(
                service.createTintedDrawable(
                    R.drawable.ic_skip_previous_white_24dp,
                    primary
                )!!,
                1.5f
            )
            val next = BitmapUtil.createBitmap(
                service.createTintedDrawable(
                    R.drawable.ic_skip_next_white_24dp,
                    primary
                )!!,
                1.5f
            )
            val playPause = BitmapUtil.createBitmap(
                service.createTintedDrawable(
                    if (service.isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp,
                    primary
                )!!,
                1.5f
            )

            notificationLayout.setTextColor(R.id.title, primary)
            notificationLayout.setTextColor(R.id.text, secondary)

            notificationLayout.setImageViewBitmap(R.id.action_prev, prev)
            notificationLayout.setImageViewBitmap(R.id.action_next, next)
            notificationLayout.setImageViewBitmap(R.id.action_play_pause, playPause)

            notificationLayoutBig.setTextColor(R.id.title, primary)
            notificationLayoutBig.setTextColor(R.id.text, secondary)
            notificationLayoutBig.setTextColor(R.id.text2, secondary)

            notificationLayoutBig.setImageViewBitmap(R.id.action_prev, prev)
            notificationLayoutBig.setImageViewBitmap(R.id.action_next, next)
            notificationLayoutBig.setImageViewBitmap(R.id.action_play_pause, playPause)
        }

    }
    //endregion

    @RequiresApi(26)
    private fun createNotificationChannel() {
        notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID).also { exist ->
            if (exist == null) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        App.instance.getString(R.string.playing_notification_name),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = App.instance.getString(
                            R.string.playing_notification_description
                        )
                        enableLights(false)
                        enableVibration(false)
                    }
                )
            }
        }
    }

    //region Misc Util
    /**
     * PendingIntent for Playback control buttons
     * @param action actions in [MusicService]
     * @return PendingIntent to operation action
     */
    private fun buildPlaybackPendingIntent(action: String): PendingIntent =
        PendingIntent.getService(
            service,
            0,
            Intent(action).apply { component = ComponentName(service, MusicService::class.java) },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

    /**
     * PendingIntent to launch MainActivity
     */
    private val clickPendingIntent: PendingIntent
        get() = PendingIntent.getActivity(
            service,
            0,
            Intent(service, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

    /**
     * PendingIntent to quit/stop
     */
    private val deletePendingIntent: PendingIntent
        get() = buildPlaybackPendingIntent(MusicService.ACTION_STOP_AND_QUIT_NOW)
    //endregion

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "playing_notification"
        private const val NOTIFICATION_ID = 1

        const val VERSION_SET_COVER_USING_METADATA = Build.VERSION_CODES.R
    }
}
