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
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.MainActivity
import player.phonograph.util.theme.createTintedDrawable
import player.phonograph.util.ui.BitmapUtil
import androidx.annotation.RequiresApi
import androidx.media.app.NotificationCompat
import android.annotation.SuppressLint
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
import android.media.MediaMetadata.*
import android.os.Build
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat as XNotificationCompat
import android.app.Notification as OSNotification

class PlayingNotificationManger(private val service: MusicService) {

    private var notificationManager: NotificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var notificationBuilder: XNotificationCompat.Builder

    init {
        notificationBuilder = XNotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(clickPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setVisibility(XNotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setPriority(XNotificationCompat.PRIORITY_MAX)
            .setCategory(XNotificationCompat.CATEGORY_TRANSPORT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel()
    }

    private lateinit var impl: Impl

    @SuppressLint("ObsoleteSdkInt")
    fun setUpNotification() {
        impl = if (!Setting.instance.classicNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Impl24()
        } else {
            Impl0()
        }
    }

    fun updateNotification() {
        val song = service.queueManager.currentSong
        if (song.id != -1L) {
            impl.update(song)
        } else {
            removeNotification()
        }
    }

    @Synchronized
    fun removeNotification() {
        service.stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun postNotification(notification: OSNotification) {
        when {
            service.isDestroyed -> {
                // service stopped
                removeNotification()
            }
            !service.isDestroyed && !service.isPlaying -> {
                // pause
                notificationManager.notify(NOTIFICATION_ID, notification)
                service.stopForeground(STOP_FOREGROUND_DETACH)
            }
            !service.isDestroyed && service.isPlaying -> {
                // playing
                notificationManager.notify(NOTIFICATION_ID, notification)
                service.startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    internal interface Impl {
        fun update(song: Song)
    }

    // Disposable ImageRequest for Cover Art
    private var request: Disposable? = null

    inner class Impl24 : Impl {
        @Synchronized
        override fun update(song: Song) {
            val isPlaying = service.isPlaying


            val playPauseAction = XNotificationCompat.Action(
                if (isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp,
                service.getString(R.string.action_play_pause),
                buildPlaybackPendingIntent(MusicService.ACTION_TOGGLE_PAUSE)
            )
            val previousAction = XNotificationCompat.Action(
                R.drawable.ic_skip_previous_white_24dp,
                service.getString(R.string.action_previous),
                buildPlaybackPendingIntent(MusicService.ACTION_REWIND)
            )
            val nextAction = XNotificationCompat.Action(
                R.drawable.ic_skip_next_white_24dp,
                service.getString(R.string.action_next),
                buildPlaybackPendingIntent(MusicService.ACTION_SKIP)
            )

            notificationBuilder
                .setContentTitle(song.title)
                .setContentText(song.artistName)
                .setSubText(song.albumName)
                .setOngoing(isPlaying)
                .setLargeIcon(service.coverLoader.defaultCover)
                .clearActions()
                .addAction(previousAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .also { builder ->
                    builder
                        .setStyle(
                            NotificationCompat.MediaStyle()
                                .setMediaSession(service.mediaSession.sessionToken)
                                .setShowActionsInCompactView(0, 1, 2)
                        )

                }

            postNotification(notificationBuilder.build())

            if (Build.VERSION.SDK_INT < VERSION_SET_COVER_USING_METADATA) {
                request?.dispose()
                request = service.coverLoader.load(song) { bitmap: Bitmap?, paletteColor: Int ->
                    if (bitmap != null) {
                        notificationBuilder
                            .setLargeIcon(bitmap)
                            .also { builder ->
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O &&
                                    Setting.instance.coloredNotification
                                ) {
                                    builder.color = paletteColor
                                }
                            }
                        postNotification(notificationBuilder.build())
                    }
                }
            }
        }
    }

    inner class Impl0 : Impl {
        @Synchronized
        override fun update(song: Song) {
            val isPlaying = service.isPlaying
            val notificationLayout = RemoteViews(service.packageName, R.layout.notification)
            val notificationLayoutBig = RemoteViews(
                service.packageName,
                R.layout.notification_big
            )

            if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(song.artistName)) {
                notificationLayout.setViewVisibility(R.id.media_titles, View.INVISIBLE)
            } else {
                notificationLayout.setViewVisibility(R.id.media_titles, View.VISIBLE)
                notificationLayout.setTextViewText(R.id.title, song.title)
                notificationLayout.setTextViewText(R.id.text, song.artistName)
            }

            if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(song.artistName) && TextUtils.isEmpty(
                    song.albumName
                )
            ) {
                notificationLayoutBig.setViewVisibility(R.id.media_titles, View.INVISIBLE)
            } else {
                notificationLayoutBig.setViewVisibility(R.id.media_titles, View.VISIBLE)
                notificationLayoutBig.setTextViewText(R.id.title, song.title)
                notificationLayoutBig.setTextViewText(R.id.text, song.artistName)
                notificationLayoutBig.setTextViewText(R.id.text2, song.albumName)
            }

            linkButtons(notificationLayout, notificationLayoutBig)

            // set default cover
            notificationLayout.setImageViewResource(R.id.image, R.drawable.default_album_art)
            notificationLayoutBig.setImageViewResource(R.id.image, R.drawable.default_album_art)
            setBackgroundColor(Color.WHITE, notificationLayout, notificationLayoutBig)
            setNotificationContent(Color.WHITE, notificationLayout, notificationLayoutBig)

            notificationBuilder
                .setContent(notificationLayout)
                .setCustomBigContentView(notificationLayoutBig)
                .setOngoing(isPlaying)

            postNotification(notificationBuilder.build())

            // then try to load cover image
            request?.dispose()
            request = service.coverLoader.load(song) { bitmap: Bitmap?, backgroundColor: Int ->
                if (bitmap != null) {
                    notificationLayout.setImageViewBitmap(R.id.image, bitmap)
                    notificationLayoutBig.setImageViewBitmap(R.id.image, bitmap)
                    if (Setting.instance.coloredNotification) {
                        setBackgroundColor(backgroundColor, notificationLayout, notificationLayoutBig)
                        setNotificationContent(backgroundColor, notificationLayout, notificationLayoutBig)
                    }
                    postNotification(notificationBuilder.build())
                }
            }
        }

        private fun setBackgroundColor(
            color: Int,
            notificationLayout: RemoteViews,
            notificationLayoutBig: RemoteViews,
        ) {
            notificationLayout.setInt(R.id.root, "setBackgroundColor", color)
            notificationLayoutBig.setInt(R.id.root, "setBackgroundColor", color)
        }

        private fun setNotificationContent(
            bgColor: Int,
            notificationLayout: RemoteViews,
            notificationLayoutBig: RemoteViews,
        ) {
            val primary = service.primaryTextColor(bgColor)
            val secondary = service.secondaryTextColor(bgColor)

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

    }

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


    /*
     * Misc
     */

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
    private val deletePendingIntent
        get() = buildPlaybackPendingIntent(
            MusicService.ACTION_STOP_AND_QUIT_NOW
        )

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "playing_notification"
        private const val NOTIFICATION_ID = 1
        const val VERSION_SET_COVER_USING_METADATA = Build.VERSION_CODES.R
    }
}
