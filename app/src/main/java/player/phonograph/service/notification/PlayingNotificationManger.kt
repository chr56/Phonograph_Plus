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
import player.phonograph.util.theme.createTintedDrawable
import player.phonograph.util.ui.BitmapUtil
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
            impl = if (value) Impl0() else Impl24()
        }
        collect(Keys.coloredNotification) { value ->
            coloredNotification = value
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
        ): XNotificationCompat.Builder {

            val base = notificationBuilder
                .setContentTitle(title)
                .setContentText(content)
                .setSubText(subText)
                .setOngoing(ongoing)
                .setLargeIcon(service.coverLoader.defaultCover)
                .clearActions()


            val status =
                ServiceStatus(service.isPlaying, service.queueManager.shuffleMode, service.queueManager.repeatMode)

            val config: NotificationActionsConfig = NotificationConfig.actions
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


        private fun emptyNotification() =
            prepareNotification(
                title = service.getString(R.string.empty),
                content = null,
                subText = null,
                ongoing = true,
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
        private fun RemoteViews.setupActionButtons() {

            val config = NotificationConfig.actions
            val actions = config.actions.map { it.notificationAction }

            for (i in actions.indices) {
                val notificationAction = actions[i] ?: continue
                setOnClickPendingIntent(
                    actionPlaceholders[i],
                    buildPlaybackPendingIntent(notificationAction.action)
                )
            }
        }

        /**
         * @return Icon Bitmap of this [action]
         */
        private fun icon(action: NotificationAction?, status: ServiceStatus, backgroundColor: Int): Bitmap =
            BitmapUtil.createBitmap(
                service.createTintedDrawable(
                    action?.icon(status) ?: R.drawable.ic_notification,
                    service.primaryTextColor(backgroundColor)
                )!!, 1.5f
            )

        private fun updateNotificationContent(
            backgroundColor: Int,
            notificationLayout: RemoteViews,
            notificationLayoutBig: RemoteViews,
        ) {
            val primary = service.primaryTextColor(backgroundColor)
            val secondary = service.secondaryTextColor(backgroundColor)

            notificationLayout.setTextColor(R.id.title, primary)
            notificationLayout.setTextColor(R.id.text, secondary)

            notificationLayoutBig.setTextColor(R.id.title, primary)
            notificationLayoutBig.setTextColor(R.id.text, secondary)
            notificationLayoutBig.setTextColor(R.id.text2, secondary)

            val config = NotificationConfig.actions
            val actions = config.actions.map { it.notificationAction }
            val status =
                ServiceStatus(service.isPlaying, service.queueManager.shuffleMode, service.queueManager.repeatMode)

            for (i in actionPlaceholders.indices) {
                val notificationAction = actions.getOrNull(i)
                if (notificationAction != null) {
                    notificationLayout.setViewVisibility(actionPlaceholders[i], View.VISIBLE)
                    notificationLayout.setImageViewBitmap(
                        actionPlaceholders[i],
                        icon(notificationAction, status, backgroundColor)
                    )
                    notificationLayoutBig.setViewVisibility(actionPlaceholders[i], View.VISIBLE)
                    notificationLayoutBig.setImageViewBitmap(
                        actionPlaceholders[i],
                        icon(notificationAction, status, backgroundColor)
                    )
                } else{
                    // hide
                    notificationLayout.setViewVisibility(actionPlaceholders[i], View.GONE)
                    notificationLayoutBig.setViewVisibility(actionPlaceholders[i], View.GONE)
                }
            }

        }

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
