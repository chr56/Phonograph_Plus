/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.notification

import android.annotation.SuppressLint
import android.app.Notification as OSNotification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaMetadata.*
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat as XNotificationCompat
import androidx.media.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.glide.BlurTransformation
import player.phonograph.glide.SongGlideRequest
import player.phonograph.glide.palette.BitmapPaletteWrapper
import player.phonograph.model.Song
import player.phonograph.service.MusicService
import player.phonograph.service.util.MediaButtonIntentReceiver
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.MainActivity
import player.phonograph.util.ImageUtil
import player.phonograph.util.ImageUtil.copy
import player.phonograph.util.PhonographColorUtil
import player.phonograph.util.Util
import util.mdcolor.ColorUtil
import util.mddesign.util.MaterialColorHelper

class PlayingNotificationManger(private val service: MusicService) {

    private var notificationManager: NotificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var notificationBuilder: XNotificationCompat.Builder

    init {
        notificationBuilder = XNotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(clickPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setPriority(XNotificationCompat.PRIORITY_MAX)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_SERVICE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel()
    }

    private val currentMode
        get() = if (service.isPlaying) MODE_FOREGROUND else MODE_BACKGROUND

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
        service.stopForeground(true)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun postNotification(notification: OSNotification) {
        if (service.isDestroyed) {
            // service stopped
            removeNotification()
            return
        }
        when (currentMode) {
            MODE_FOREGROUND -> {
                notificationManager.notify(NOTIFICATION_ID, notification)
                service.startForeground(NOTIFICATION_ID, notification)
            }
            MODE_BACKGROUND -> {
                notificationManager.notify(NOTIFICATION_ID, notification)
                service.stopForeground(false)
            }
        }
    }

    internal interface Impl {
        fun update(song: Song)
    }

    inner class Impl24 : Impl {
        @Synchronized
        override fun update(song: Song) {
            val isPlaying = service.isPlaying

            val bigNotificationImageSize = service.resources.getDimensionPixelSize(
                R.dimen.notification_big_image_size
            )

            val playPauseAction = androidx.core.app.NotificationCompat.Action(
                if (isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp,
                service.getString(R.string.action_play_pause),
                buildPlaybackPendingIntent(MusicService.ACTION_TOGGLE_PAUSE)
            )
            val previousAction = androidx.core.app.NotificationCompat.Action(
                R.drawable.ic_skip_previous_white_24dp,
                service.getString(R.string.action_previous),
                buildPlaybackPendingIntent(MusicService.ACTION_REWIND)
            )
            val nextAction = androidx.core.app.NotificationCompat.Action(
                R.drawable.ic_skip_next_white_24dp,
                service.getString(R.string.action_next),
                buildPlaybackPendingIntent(MusicService.ACTION_SKIP)
            )

            val defaultCover = BitmapFactory.decodeResource(
                service.resources,
                R.drawable.default_album_art
            )

            notificationBuilder
                .setContentTitle(song.title)
                .setContentText(song.artistName)
                .setSubText(song.albumName)
                .setOngoing(isPlaying)
                .setLargeIcon(defaultCover)
                .clearActions()
                .addAction(previousAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .also { builder ->
                    /* noinspection ObsoleteSdkInt*/
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        builder
                            .setStyle(
                                NotificationCompat.MediaStyle()
                                    .setMediaSession(mediaSession.sessionToken)
                                    .setShowActionsInCompactView(0, 1, 2)
                            )
                    }
                }

            postNotification(notificationBuilder.build())

            // then try to load cover image

            service.runOnUiThread {
                SongGlideRequest.Builder.from(Glide.with(service), song)
                    .checkIgnoreMediaStore(service)
                    .generatePalette(service).build()
                    .into(object : CustomTarget<BitmapPaletteWrapper>(
                        bigNotificationImageSize,
                        bigNotificationImageSize
                    ) {

                            override fun onResourceReady(
                                resource: BitmapPaletteWrapper,
                                transition: Transition<in BitmapPaletteWrapper>?
                            ) {
                                updateCover(
                                    resource.bitmap,
                                    PhonographColorUtil.getColor(
                                        resource.palette,
                                        Color.TRANSPARENT
                                    )
                                )
                            }

                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                updateCover(null, Color.TRANSPARENT)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                updateCover(null, Color.WHITE)
                            }

                            fun updateCover(bitmap: Bitmap?, color: Int) {
                                notificationBuilder
                                    .setLargeIcon(bitmap ?: defaultCover)
                                    .also { builder ->
                                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O && Setting.instance.coloredNotification) builder.color = color
                                    }
                                postNotification(notificationBuilder.build())
                            }
                        })
            }
        }
    }
    inner class Impl0 : Impl {
        private var target: Target<BitmapPaletteWrapper>? = null

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
            setNotificationContent(true, notificationLayout, notificationLayoutBig)

            notificationBuilder
                .setContent(notificationLayout)
                .setCustomBigContentView(notificationLayoutBig)
                .setOngoing(isPlaying)

            postNotification(notificationBuilder.build())

            // then try to load cover image

            service.runOnUiThread {
                if (target != null) {
                    Glide.with(service).clear(target)
                }

                target = SongGlideRequest.Builder.from(Glide.with(service), song)
                    .checkIgnoreMediaStore(service).generatePalette(service).build()
                    .into(object : CustomTarget<BitmapPaletteWrapper>(
                        bigNotificationImageSize,
                        bigNotificationImageSize
                    ) {

                            override fun onResourceReady(
                                resource: BitmapPaletteWrapper,
                                transition: Transition<in BitmapPaletteWrapper>?
                            ) {
                                updateCover(
                                    resource.bitmap,
                                    PhonographColorUtil.getColor(
                                        resource.palette,
                                        Color.TRANSPARENT
                                    )
                                )
                            }

                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                updateCover(null, Color.WHITE)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                updateCover(null, Color.WHITE)
                            }

                            private fun updateCover(bitmap: Bitmap?, backgroundColor: Int) {
                                if (bitmap != null) {
                                    notificationLayout.setImageViewBitmap(R.id.image, bitmap)
                                    notificationLayoutBig.setImageViewBitmap(R.id.image, bitmap)
                                }
                                if (Setting.instance.coloredNotification) {
                                    setBackgroundColor(
                                        backgroundColor,
                                        notificationLayout,
                                        notificationLayoutBig
                                    )
                                    setNotificationContent(
                                        ColorUtil.isColorLight(backgroundColor),
                                        notificationLayout,
                                        notificationLayoutBig
                                    )
                                }

                                postNotification(notificationBuilder.build())
                            }
                        })
            }
        }

        private fun setBackgroundColor(
            color: Int,
            notificationLayout: RemoteViews,
            notificationLayoutBig: RemoteViews
        ) {
            notificationLayout.setInt(R.id.root, "setBackgroundColor", color)
            notificationLayoutBig.setInt(R.id.root, "setBackgroundColor", color)
        }

        private fun setNotificationContent(
            dark: Boolean,
            notificationLayout: RemoteViews,
            notificationLayoutBig: RemoteViews
        ) {
            val primary = MaterialColorHelper.getPrimaryTextColor(service, dark)
            val secondary = MaterialColorHelper.getSecondaryTextColor(service, dark)

            val prev = ImageUtil.createBitmap(
                ImageUtil.getTintedVectorDrawable(
                    service,
                    R.drawable.ic_skip_previous_white_24dp,
                    primary
                ),
                1.5f
            )
            val next = ImageUtil.createBitmap(
                ImageUtil.getTintedVectorDrawable(
                    service,
                    R.drawable.ic_skip_next_white_24dp,
                    primary
                ),
                1.5f
            )
            val playPause = ImageUtil.createBitmap(
                ImageUtil.getTintedVectorDrawable(
                    service,
                    if (service.isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp,
                    primary
                ),
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

        val bigNotificationImageSize by lazy {
            service.resources.getDimensionPixelSize(
                R.dimen.notification_big_image_size
            )
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

    lateinit var mediaSession: MediaSessionCompat private set

    fun setupMediaSession(callback: MediaSessionCompat.Callback) {
        val mediaButtonReceiverComponentName = ComponentName(
            service.applicationContext,
            MediaButtonIntentReceiver::class.java
        )
        val mediaButtonReceiverPendingIntent =
            PendingIntent.getBroadcast(
                service.applicationContext,
                0,
                Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    component = mediaButtonReceiverComponentName
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        mediaSession =
            MediaSessionCompat(
                service,
                BuildConfig.APPLICATION_ID,
                mediaButtonReceiverComponentName,
                mediaButtonReceiverPendingIntent
            )
        mediaSession.setCallback(callback)

        // fixme remove deprecation
        @Suppress("DEPRECATION")
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
        )
        mediaSession.setMediaButtonReceiver(mediaButtonReceiverPendingIntent)
    }

    fun updateMediaSessionPlaybackState() {
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_STOP
                        or PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(
                    if (service.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    service.songProgressMillis.toLong(),
                    1f
                )
                .build()
        )
    }

    @SuppressLint("CheckResult")
    fun updateMediaSessionMetaData() {
        val queueManager = service.queueManager
        val song = queueManager.currentSong
        if (song.id == -1L) {
            mediaSession.setMetadata(null)
            return
        }
        val metaData =
            MediaMetadataCompat.Builder().apply {
                putString(METADATA_KEY_ARTIST, song.artistName)
                putString(METADATA_KEY_ALBUM_ARTIST, song.artistName)
                putString(METADATA_KEY_ALBUM, song.albumName)
                putString(METADATA_KEY_TITLE, song.title)
                putLong(METADATA_KEY_DURATION, song.duration)
                putLong(METADATA_KEY_TRACK_NUMBER, (queueManager.currentSongPosition + 1).toLong())
                putLong(METADATA_KEY_YEAR, song.year.toLong())
                putBitmap(METADATA_KEY_ALBUM_ART, null)
                putLong(
                    METADATA_KEY_NUM_TRACKS,
                    service.queueManager.playingQueue.size.toLong()
                )
            }

        if (Setting.instance.albumArtOnLockscreen) {
            val screenSize = Util.getScreenSize(service)
            val request = SongGlideRequest.Builder.from(Glide.with(service), song)
                .checkIgnoreMediaStore(service)
                .asBitmap().build().also {
                    if (Setting.instance.blurredAlbumArt) it.transform(
                        BlurTransformation.Builder(service).build()
                    )
                }

            service.runOnUiThread {
                request.into(
                    object : CustomTarget<Bitmap>(screenSize.x, screenSize.y) {

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            mediaSession.setMetadata(metaData.build())
                        }

                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            metaData.putBitmap(
                                METADATA_KEY_ALBUM_ART,
                                resource.copy()
                            )
                            mediaSession.setMetadata(metaData.build())
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            mediaSession.setMetadata(metaData.build()) // todo check leakage
                        }
                    }
                )
            }
        } else {
            mediaSession.setMetadata(metaData.build())
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
    private val clickPendingIntent: PendingIntent get() = PendingIntent.getActivity(
        service,
        0,
        Intent(service, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
    )

    /**
     * PendingIntent to quit/stop
     */
    private val deletePendingIntent get() = buildPlaybackPendingIntent(MusicService.ACTION_QUIT)

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "playing_notification"
        private const val NOTIFICATION_ID = 1
        private const val MODE_FOREGROUND = 1
        private const val MODE_BACKGROUND = 0
    }
}
