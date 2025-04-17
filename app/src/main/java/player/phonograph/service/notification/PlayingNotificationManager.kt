/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.notification

import coil.request.Disposable
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.notification.NotificationAction
import player.phonograph.model.notification.NotificationActionsConfig
import player.phonograph.model.service.ACTION_STOP_AND_QUIT_NOW
import player.phonograph.model.service.MusicServiceStatus
import player.phonograph.model.service.PlayerState.PAUSED
import player.phonograph.model.service.PlayerState.PLAYING
import player.phonograph.model.service.PlayerState.PREPARING
import player.phonograph.model.service.PlayerState.STOPPED
import player.phonograph.service.MusicService
import player.phonograph.service.ServiceComponent
import player.phonograph.settings.Keys
import player.phonograph.settings.SettingObserver
import player.phonograph.ui.modules.main.MainActivity
import player.phonograph.util.permissions.checkNotificationPermission
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.ui.BitmapUtil
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import androidx.annotation.LayoutRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.app.Notification
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
import kotlinx.coroutines.yield

class PlayingNotificationManager : ServiceComponent {
    override var created: Boolean = false

    private var _service: MusicService? = null
    private val service: MusicService get() = _service!!

    private lateinit var notificationManager: NotificationManagerCompat

    private lateinit var settingObserver: SettingObserver

    private var classicNotification: Boolean = false
    private var coloredNotification: Boolean = true

    private var actionsConfig: NotificationActionsConfig? = null

    var persistent: Boolean = false
        private set

    private var implementation: Implementation? = null

    override fun onCreate(musicService: MusicService) {
        _service = musicService


        notificationManager = NotificationManagerCompat.from(musicService)

        val channel: NotificationChannelCompat? =
            notificationManager.getNotificationChannelCompat(NOTIFICATION_CHANNEL_ID)
        if (channel == null) {
            notificationManager.createNotificationChannel(
                NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(musicService.getString(R.string.playing_notification_name))
                    .setDescription(musicService.getString(R.string.playing_notification_description))
                    .setLightsEnabled(false)
                    .setVibrationEnabled(false)
                    .build()
            )
        }

        settingObserver = SettingObserver(service, service.coroutineScope)

        settingObserver.collect(Keys.classicNotification) { value ->
            classicNotification = value
            implementation = if (value) ClassicNotification() else MediaStyleNotification()
        }
        settingObserver.collect(Keys.coloredNotification) { value ->
            coloredNotification = value
        }
        settingObserver.collect(Keys.notificationActions) { config ->
            actionsConfig = config
        }
        settingObserver.collect(Keys.persistentPlaybackNotification) { value ->
            persistent = value
            while (implementation == null) yield()
            if (value) { //todo
                updateNotification(service.queueManager.currentSong, service.statusForNotification)
            }
        }

        implementation = if (classicNotification) ClassicNotification() else MediaStyleNotification()

        created = true
    }

    override fun onDestroy(musicService: MusicService) {
        request?.dispose()
        removeNotification()
        created = false
        implementation = null
        _service = null
    }


    private fun notificationBuilder(context: Context): NotificationCompat.Builder =
        NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(clickPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)

    private var lastSong: Song? = null
    private var lastServiceStatus: MusicServiceStatus? = null

    fun updateNotification(song: Song?, status: MusicServiceStatus) {
        if (song != null) {
            if (song != lastSong || status != lastServiceStatus) {
                val actions = actionsConfig ?: settingObserver.blocking(Keys.notificationActions)
                // Only update notification for actual changes
                lastSong = song
                lastServiceStatus = status
                implementation?.update(song, status, actions)
            }
        } else {
            if (persistent) {
                val actions = actionsConfig ?: settingObserver.blocking(Keys.notificationActions)
                implementation?.empty(status, actions)
            } else {
                removeNotification()
            }
        }
    }

    @Synchronized
    private fun postNotification(notification: Notification) {
        checkNotificationPermission(service)
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

    internal interface Implementation {
        fun update(song: Song, status: MusicServiceStatus, config: NotificationActionsConfig)
        fun empty(status: MusicServiceStatus, config: NotificationActionsConfig)
    }

    /** Disposable ImageRequest for Cover Art **/
    private var request: Disposable? = null

    /**
     * Modern Notification with [MediaStyle], requiring API 24
     */
    inner class MediaStyleNotification : Implementation {

        private fun mediaStyle(): MediaStyle =
            MediaStyle().setMediaSession(service.mediaSession.sessionToken)

        private var cachedSong: Song? = null
        private var cachedBitmap: Bitmap? = null
        private var cachedPaletteColor: Int = -1

        override fun update(song: Song, status: MusicServiceStatus, config: NotificationActionsConfig) {
            val notificationBuilder = prepareNotification(
                builder = notificationBuilder(service),
                title = song.title,
                content = song.artistName,
                subText = song.albumName,
                config = config,
                status = status,
            )

            if (SDK_INT < VERSION_SET_COVER_USING_METADATA && cachedSong == song) {
                prepareNotificationImages(notificationBuilder, cachedBitmap, cachedPaletteColor)
            }

            postNotification(notificationBuilder.build())

            request?.dispose()
            if (SDK_INT < VERSION_SET_COVER_USING_METADATA && cachedSong != song) {
                request = service.coverLoader.load(song) { bitmap: Bitmap?, paletteColor: Int ->
                    if (bitmap != null) {
                        prepareNotificationImages(notificationBuilder, bitmap, paletteColor)
                        postNotification(notificationBuilder.build())

                        this.cachedSong = song
                        this.cachedBitmap = bitmap
                        this.cachedPaletteColor = paletteColor
                    }
                }
            }
        }

        /**
         * prepare notification
         */
        private fun prepareNotification(
            builder: NotificationCompat.Builder,
            title: String,
            content: String?,
            subText: String?,
            config: NotificationActionsConfig,
            status: MusicServiceStatus,
        ): NotificationCompat.Builder {

            val base = builder
                .setContentTitle(title)
                .setContentText(content)
                .setSubText(subText)
                .setOngoing(status.isPlaying)
                .setLargeIcon(service.coverLoader.defaultCover)
                .clearActions()

            val actions =
                config.actions.map { processActions(it.notificationAction, status) }

            val positions =
                config.actions.mapIndexed { index, item ->
                    if (item.displayInCompat) index else -1
                }.filter { it > 0 }.toIntArray()

            return base
                .apply {
                    for (action in actions) addAction(action)
                }
                .setStyle(mediaStyle().setShowActionsInCompactView(*positions))
        }

        private fun processActions(action: NotificationAction?, status: MusicServiceStatus): Action {
            return if (action != null)
                Action(
                    action.icon(status),
                    service.getString(action.stringRes),
                    buildPlaybackPendingIntent(action.action)
                )
            else {
                Action(R.drawable.ic_notification, null, null)
            }
        }

        private fun prepareNotificationImages(
            notificationBuilder: NotificationCompat.Builder,
            bitmap: Bitmap?,
            paletteColor: Int,
        ): NotificationCompat.Builder {
            return if (bitmap != null) {
                notificationBuilder
                    .setLargeIcon(bitmap)
                    .also { builder ->
                        if (coloredNotification) {
                            if (paletteColor > 0) builder.color = paletteColor
                        }
                    }
            } else {
                notificationBuilder
            }
        }

        private fun emptyNotification(
            status: MusicServiceStatus,
            config: NotificationActionsConfig,
        ): NotificationCompat.Builder =
            prepareNotification(
                builder = notificationBuilder(service),
                title = service.getString(R.string.empty),
                content = null,
                subText = null,
                config = config,
                status = status
            )

        override fun empty(status: MusicServiceStatus, config: NotificationActionsConfig) {
            postNotification(emptyNotification(status, config).build())
        }
    }

    /**
     * Classic Notification using [RemoteViews]
     */
    inner class ClassicNotification : Implementation {

        private var cachedSong: Song? = null
        private var cachedBitmap: Bitmap? = null
        private var cachedPaletteColor: Int = -1

        private fun common(
            notificationBuilder: NotificationCompat.Builder,
            title: String?,
            text1: String?,
            text2: String?,
            backgroundColor: Int,
            song: Song?,
            status: MusicServiceStatus,
            config: NotificationActionsConfig,
        ) {

            val layoutCompat: RemoteViews =
                buildRemoteViews(
                    R.layout.notification,
                    title, text1, text2, backgroundColor,
                    hasText2 = false, config
                )

            val layoutExpanded: RemoteViews =
                buildRemoteViews(
                    R.layout.notification_big,
                    title, text1, text2, backgroundColor,
                    hasText2 = true, config
                )

            val primaryTextColor = service.primaryTextColor(backgroundColor)
            val secondaryTextColor = service.secondaryTextColor(backgroundColor)

            layoutCompat.updateNotificationTextColor(primaryTextColor, secondaryTextColor, false)
            layoutExpanded.updateNotificationTextColor(primaryTextColor, secondaryTextColor, true)

            layoutCompat.updateNotificationAction(backgroundColor, status, config, true)
            layoutExpanded.updateNotificationAction(backgroundColor, status, config, false)

            notificationBuilder
                .setContent(layoutCompat)
                .setCustomBigContentView(layoutExpanded)
                .setOngoing(status.isPlaying)

            if (song != null && cachedSong == song) {

                val bitmap = cachedBitmap
                if (bitmap != null) {
                    layoutCompat.updateNotificationImages(bitmap, cachedPaletteColor, status, config, true)
                    layoutExpanded.updateNotificationImages(bitmap, cachedPaletteColor, status, config, false)
                }
            }

            postNotification(notificationBuilder.build())

            request?.dispose()
            if (song != null && cachedSong != song) {
                request = service.coverLoader.load(song) { bitmap: Bitmap?, color: Int ->
                    if (bitmap != null) {
                        layoutCompat.updateNotificationImages(bitmap, color, status, config, true)
                        layoutExpanded.updateNotificationImages(bitmap, color, status, config, false)
                        postNotification(notificationBuilder.build())

                        this.cachedSong = song
                        this.cachedBitmap = bitmap
                        this.cachedPaletteColor = color

                    }
                }
            }
        }

        override fun empty(status: MusicServiceStatus, config: NotificationActionsConfig) {
            common(
                notificationBuilder = notificationBuilder(service),
                title = service.getString(R.string.empty), text1 = null, text2 = null,
                backgroundColor = Color.LTGRAY,
                song = null, status = status,
                config = config
            )
        }

        override fun update(song: Song, status: MusicServiceStatus, config: NotificationActionsConfig) {
            common(
                notificationBuilder = notificationBuilder(service),
                title = song.title, text1 = song.artistName, text2 = song.albumName,
                backgroundColor = Color.WHITE,
                song = song, status = status,
                config = config
            )
        }

        private fun buildRemoteViews(
            @LayoutRes layout: Int,
            title: String?,
            text1: String?,
            text2: String?,
            backgroundColor: Int,
            hasText2: Boolean,
            actionsConfig: NotificationActionsConfig,
        ): RemoteViews = RemoteViews(service.packageName, layout).apply {
            //region Text
            val hideTitle =
                if (hasText2) TextUtils.isEmpty(title) && TextUtils.isEmpty(text1) && TextUtils.isEmpty(text2)
                else TextUtils.isEmpty(title) && TextUtils.isEmpty(text1)

            if (hideTitle) {
                setViewVisibility(R.id.media_titles, View.INVISIBLE)
            } else {
                setViewVisibility(R.id.media_titles, View.VISIBLE)
                setTextViewText(R.id.title, title)
                setTextViewText(R.id.text, text1)
                if (hasText2) setTextViewText(R.id.text2, text2)
            }
            //endregion
            //region Default Artwork
            setImageViewResource(R.id.image, R.drawable.default_album_art)
            //endregion
            //region Color
            setBackgroundColor(backgroundColor)
            //endregion
            //region Actions
            setupActionButtons(actionsConfig)
            //endregion
        }

        private fun RemoteViews.setBackgroundColor(color: Int) =
            setInt(R.id.root, "setBackgroundColor", color)

        private fun RemoteViews.updateNotificationTextColor(
            primaryTextColor: Int,
            secondaryTextColor: Int,
            hasText2: Boolean,
        ) {
            setTextColor(R.id.title, primaryTextColor)
            setTextColor(R.id.text, secondaryTextColor)
            if (hasText2) {
                setTextColor(R.id.text2, secondaryTextColor)
            }
        }


        private fun RemoteViews.updateNotificationImages(
            bitmap: Bitmap,
            color: Int,
            status: MusicServiceStatus,
            config: NotificationActionsConfig,
            compatMode: Boolean,
        ) {
            setImageViewBitmap(R.id.image, bitmap)
            if (coloredNotification && color > 0) {
                setBackgroundColor(color)
                updateNotificationAction(color, status, config, compatMode)
            }
        }

        private val actionPlaceholders: List<Int> = listOf(
            R.id.action_placeholder1,
            R.id.action_placeholder2,
            R.id.action_placeholder3,
            R.id.action_placeholder4,
            R.id.action_placeholder5,
        )

        /**
         * Link intent with action buttons
         */
        private fun RemoteViews.setupActionButtons(config: NotificationActionsConfig) {
            val actions = config.actions.map { it.notificationAction }
            for (i in actionPlaceholders.indices) {
                val notificationAction = actions.getOrNull(i) ?: continue
                setOnClickPendingIntent(
                    actionPlaceholders[i],
                    buildPlaybackPendingIntent(notificationAction.action)
                )
            }
        }

        private fun RemoteViews.updateNotificationAction(
            backgroundColor: Int,
            status: MusicServiceStatus,
            config: NotificationActionsConfig,
            compatMode: Boolean,
        ) {
            val actions =
                if (compatMode)
                    config.actions.filter { it.displayInCompat }.map { it.notificationAction }
                else
                    config.actions.map { it.notificationAction }

            for (i in actionPlaceholders.indices) {
                val notificationAction = actions.getOrNull(i)
                if (notificationAction != null) {
                    setViewVisibility(actionPlaceholders[i], View.VISIBLE)
                    setImageViewBitmap(
                        actionPlaceholders[i],
                        icon(notificationAction, status, backgroundColor)
                    )
                } else {
                    // hide
                    setViewVisibility(actionPlaceholders[i], View.GONE)
                }
            }

        }

        /**
         * @return Icon Bitmap of this [action]
         */
        private fun icon(action: NotificationAction, status: MusicServiceStatus, backgroundColor: Int): Bitmap =
            BitmapUtil.createBitmap(
                service.getTintedDrawable(action.icon(status), service.primaryTextColor(backgroundColor))!!, 1.5f
            )

    }
    //endregion

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
            MainActivity.launchingIntent(service, Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

    /**
     * PendingIntent to quit/stop
     */
    private val deletePendingIntent: PendingIntent
        get() = buildPlaybackPendingIntent(ACTION_STOP_AND_QUIT_NOW)
    //endregion

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "playing_notification"
        private const val NOTIFICATION_ID = 1

        const val VERSION_SET_COVER_USING_METADATA = Build.VERSION_CODES.R
    }
}
