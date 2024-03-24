/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.notification

import coil.request.Disposable
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.mechanism.setting.NotificationAction
import player.phonograph.mechanism.setting.NotificationActionsConfig
import player.phonograph.mechanism.setting.NotificationConfig
import player.phonograph.model.Song
import player.phonograph.service.MusicService
import player.phonograph.service.ServiceComponent
import player.phonograph.service.ServiceStatus
import player.phonograph.service.player.PlayerState.PAUSED
import player.phonograph.service.player.PlayerState.PLAYING
import player.phonograph.service.player.PlayerState.PREPARING
import player.phonograph.service.player.PlayerState.STOPPED
import player.phonograph.settings.Keys
import player.phonograph.settings.PrimitiveKey
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.MainActivity
import player.phonograph.util.permissions.checkNotificationPermission
import player.phonograph.util.theme.createTintedDrawable
import player.phonograph.util.ui.BitmapUtil
import androidx.annotation.LayoutRes
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
import kotlinx.coroutines.yield
import androidx.core.app.NotificationCompat as XNotificationCompat
import android.app.Notification as OSNotification

class PlayingNotificationManger : ServiceComponent {

    private var _service: MusicService? = null
    private val service: MusicService get() = _service!!

    private lateinit var notificationManager: NotificationManager

    private var classicNotification: Boolean = false
    private var coloredNotification: Boolean = true

    private lateinit var actionsConfig: NotificationActionsConfig

    var persistent: Boolean = false
        private set

    private var impl: Impl? = null

    override fun onCreate(musicService: MusicService) {
        _service = musicService


        notificationManager =
            musicService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        actionsConfig = NotificationConfig.actions

        fun <T> collect(key: PrimitiveKey<T>, collector: FlowCollector<T>) {
            service.coroutineScope.launch(SupervisorJob()) {
                Setting(musicService)[key].flow.distinctUntilChanged().collect(collector)
            }
        }
        collect(Keys.classicNotification) { value ->
            classicNotification = value
            impl = if (value) Impl0() else Impl24()
        }
        collect(Keys.coloredNotification) { value ->
            coloredNotification = value
        }
        collect(Keys.notificationActionsJsonString) { _ ->
            actionsConfig = NotificationConfig.actions
        }
        collect(Keys.persistentPlaybackNotification) { value ->
            persistent = value
            while (impl == null) yield()
            if (value) {
                updateNotification(service.queueManager.currentSong)
            }
        }

        impl = if (classicNotification) Impl0() else Impl24()
    }

    override fun onDestroy(musicService: MusicService) {
        removeNotification()
        impl = null
        _service = null
    }


    private fun notificationBuilder(context: Context): XNotificationCompat.Builder =
        XNotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(clickPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setVisibility(XNotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setPriority(XNotificationCompat.PRIORITY_MAX)
            .setCategory(XNotificationCompat.CATEGORY_TRANSPORT)

    fun updateNotification(song: Song) {
        if (song.id != -1L) {
            impl?.update(song, actionsConfig)
        } else {
            if (persistent) {
                impl?.empty(actionsConfig)
            } else {
                removeNotification()
            }
        }
    }

    @Synchronized
    private fun postNotification(notification: OSNotification) {
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

    internal interface Impl {
        fun update(song: Song, config: NotificationActionsConfig)
        fun empty(config: NotificationActionsConfig)
    }

    /** Disposable ImageRequest for Cover Art **/
    private var request: Disposable? = null

    inner class Impl24 : Impl {

        private fun mediaStyle(): NotificationCompat.MediaStyle =
            NotificationCompat.MediaStyle().setMediaSession(service.mediaSession.sessionToken)

        private var cachedSong: Song? = null
        private var cachedBitmap: Bitmap? = null
        private var cachedPaletteColor: Int = -1

        override fun update(song: Song, config: NotificationActionsConfig) {
            val notificationBuilder = prepareNotification(
                builder = notificationBuilder(service),
                title = song.title,
                content = song.artistName,
                subText = song.albumName,
                config = config,
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
            builder: XNotificationCompat.Builder,
            title: String,
            content: String?,
            subText: String?,
            config: NotificationActionsConfig,
        ): XNotificationCompat.Builder {

            val status =
                ServiceStatus(service.isPlaying, service.queueManager.shuffleMode, service.queueManager.repeatMode)

            val base = builder
                .setContentTitle(title)
                .setContentText(content)
                .setSubText(subText)
                .setOngoing(service.isPlaying)
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

        private fun processActions(action: NotificationAction?, status: ServiceStatus): XNotificationCompat.Action {
            return if (action != null)
                XNotificationCompat.Action(
                    action.icon(status),
                    service.getString(action.stringRes),
                    buildPlaybackPendingIntent(action.action)
                )
            else {
                XNotificationCompat.Action(R.drawable.ic_notification, null, null)
            }
        }

        private fun prepareNotificationImages(
            notificationBuilder: XNotificationCompat.Builder,
            bitmap: Bitmap?,
            paletteColor: Int,
        ): XNotificationCompat.Builder {
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

        private fun emptyNotification(config: NotificationActionsConfig): XNotificationCompat.Builder =
            prepareNotification(
                builder = notificationBuilder(service),
                title = service.getString(R.string.empty),
                content = null,
                subText = null,
                config = config
            )

        override fun empty(config: NotificationActionsConfig) {
            postNotification(emptyNotification(config).build())
        }
    }

    inner class Impl0 : Impl {

        private var cachedSong: Song? = null
        private var cachedBitmap: Bitmap? = null
        private var cachedPaletteColor: Int = -1

        private fun common(
            notificationBuilder: XNotificationCompat.Builder,
            title: String?,
            text1: String?,
            text2: String?,
            backgroundColor: Int,
            ongoing: Boolean,
            song: Song?,
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

            layoutCompat.updateNotificationAction(backgroundColor, config, true)
            layoutExpanded.updateNotificationAction(backgroundColor, config, false)

            notificationBuilder
                .setContent(layoutCompat)
                .setCustomBigContentView(layoutExpanded)
                .setOngoing(ongoing)

            if (song != null && cachedSong == song) {

                val bitmap = cachedBitmap
                if (bitmap != null) {
                    layoutCompat.updateNotificationImages(bitmap, cachedPaletteColor, config, true)
                    layoutExpanded.updateNotificationImages(bitmap, cachedPaletteColor, config, false)
                }
            }

            postNotification(notificationBuilder.build())

            request?.dispose()
            if (song != null && cachedSong != song) {
                request = service.coverLoader.load(song) { bitmap: Bitmap?, color: Int ->
                    if (bitmap != null) {
                        layoutCompat.updateNotificationImages(bitmap, color, config, true)
                        layoutExpanded.updateNotificationImages(bitmap, color, config, false)
                        postNotification(notificationBuilder.build())

                        this.cachedSong = song
                        this.cachedBitmap = bitmap
                        this.cachedPaletteColor = color

                    }
                }
            }
        }

        override fun empty(config: NotificationActionsConfig) {
            common(
                notificationBuilder = notificationBuilder(service),
                title = service.getString(R.string.empty), text1 = null, text2 = null,
                backgroundColor = Color.LTGRAY,
                ongoing = true,
                song = null,
                config = config
            )
        }

        override fun update(song: Song, config: NotificationActionsConfig) {
            common(
                notificationBuilder = notificationBuilder(service),
                title = song.title, text1 = song.artistName, text2 = song.albumName,
                backgroundColor = Color.WHITE,
                ongoing = service.isPlaying,
                song = song,
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
            config: NotificationActionsConfig,
            compatMode: Boolean,
        ) {
            setImageViewBitmap(R.id.image, bitmap)
            if (coloredNotification && color > 0) {
                setBackgroundColor(color)
                updateNotificationAction(color, config, compatMode)
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
            config: NotificationActionsConfig,
            compatMode: Boolean,
        ) {
            val actions =
                if (compatMode)
                    config.actions.filter { it.displayInCompat }.map { it.notificationAction }
                else
                    config.actions.map { it.notificationAction }

            val status =
                ServiceStatus(service.isPlaying, service.queueManager.shuffleMode, service.queueManager.repeatMode)

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
        private fun icon(action: NotificationAction, status: ServiceStatus, backgroundColor: Int): Bitmap =
            BitmapUtil.createBitmap(
                service.createTintedDrawable(action.icon(status), service.primaryTextColor(backgroundColor))!!, 1.5f
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
