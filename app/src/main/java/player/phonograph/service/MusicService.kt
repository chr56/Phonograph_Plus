/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.service

import lib.phonograph.localization.ContextLocaleDelegate
import org.koin.android.ext.android.get
import player.phonograph.ACTUAL_PACKAGE_NAME
import player.phonograph.BuildConfig
import player.phonograph.MusicServiceMsgConst.META_CHANGED
import player.phonograph.MusicServiceMsgConst.PLAY_STATE_CHANGED
import player.phonograph.MusicServiceMsgConst.QUEUE_CHANGED
import player.phonograph.MusicServiceMsgConst.REPEAT_MODE_CHANGED
import player.phonograph.MusicServiceMsgConst.SHUFFLE_MODE_CHANGED
import player.phonograph.appwidgets.AppWidgetBig
import player.phonograph.appwidgets.AppWidgetCard
import player.phonograph.appwidgets.AppWidgetClassic
import player.phonograph.appwidgets.AppWidgetSmall
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.repo.browser.MediaBrowserDelegate
import player.phonograph.repo.database.HistoryStore
import player.phonograph.service.notification.CoverLoader
import player.phonograph.service.notification.PlayingNotificationManger
import player.phonograph.service.player.MSG_NOW_PLAYING_CHANGED
import player.phonograph.service.player.MediaSessionController
import player.phonograph.service.player.PlayerController
import player.phonograph.service.player.PlayerController.ControllerHandler.Companion.RE_PREPARE_NEXT_PLAYER
import player.phonograph.service.player.PlayerState
import player.phonograph.service.player.PlayerStateObserver
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.QueueManager.Companion.MSG_SAVE_CFG
import player.phonograph.service.queue.QueueManager.Companion.MSG_SAVE_QUEUE
import player.phonograph.service.queue.QueueObserver
import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.service.util.MediaStoreObserverUtil
import player.phonograph.service.util.MusicServiceUtil
import player.phonograph.service.util.SongPlayCountHelper
import player.phonograph.settings.Keys
import player.phonograph.settings.PrimitiveKey
import player.phonograph.settings.Setting
import player.phonograph.util.recordThrowable
import player.phonograph.util.registerReceiverCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.audiofx.AudioEffect
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * @author Karim Abou Zeid (kabouzeid), Andrew Neal
 */
class MusicService : MediaBrowserServiceCompat() {

    private val songPlayCountHelper = SongPlayCountHelper()

    val queueManager: QueueManager = get()
    private val queueChangeObserver: QueueObserver = initQueueChangeObserver()

    private val controller: PlayerController = PlayerController()
    private var playerStateObserver: PlayerStateObserver = initPlayerStateObserver()

    private val playNotificationManager: PlayingNotificationManger = PlayingNotificationManger()

    private val mediaSessionController: MediaSessionController = MediaSessionController()

    private lateinit var throttledTimer: ThrottledTimer

    private val mediaStoreObserverUtil = MediaStoreObserverUtil()

    lateinit var coverLoader: CoverLoader

    val coroutineScope get() = _coroutineScope!!
    private var _coroutineScope: CoroutineScope? = null

    override fun onCreate() {

        _coroutineScope = CoroutineScope(Dispatchers.IO)
        super.onCreate()

        // controller
        controller.onCreate(this)

        // observers & messages
        sendChangeInternal(META_CHANGED) // notify manually for first setting up queueManager
        sendChangeInternal(QUEUE_CHANGED) // notify manually for first setting up queueManager
        queueManager.addObserver(queueChangeObserver)
        controller.addObserver(playerStateObserver)

        // notifications & media session
        coverLoader = CoverLoader(this)
        mediaSessionController.onCreate(this)
        playNotificationManager.onCreate(this)
        sessionToken = mediaSessionController.mediaSession.sessionToken // MediaBrowserService

        mediaSessionController.mediaSession.isActive = true

        // process updater
        throttledTimer = ThrottledTimer(controller.handler)

        // misc
        observeSettings()
        mediaStoreObserverUtil.setUpMediaStoreObserver(
            this,
            controller.handler, // todo use other handler
            this@MusicService::handleAndSendChangeInternal
        )
        registerReceiverCompat(
            widgetIntentReceiver,
            IntentFilter(APP_WIDGET_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        sendBroadcast(Intent("player.phonograph.PHONOGRAPH_MUSIC_SERVICE_CREATED"))
    }

    private fun initQueueChangeObserver(): QueueObserver = object : QueueObserver {
        override fun onCurrentPositionChanged(newPosition: Int) {
            notifyChange(META_CHANGED)
            rePrepareNextSong()
        }

        override fun onQueueChanged(newPlayingQueue: List<Song>, newOriginalQueue: List<Song>) {
            handleAndSendChangeInternal(QUEUE_CHANGED)
            notifyChange(META_CHANGED)
            rePrepareNextSong()
        }

        override fun onShuffleModeChanged(newMode: ShuffleMode) {
            rePrepareNextSong()
            handleAndSendChangeInternal(SHUFFLE_MODE_CHANGED)
        }

        override fun onRepeatModeChanged(newMode: RepeatMode) {
            rePrepareNextSong()
            handleAndSendChangeInternal(REPEAT_MODE_CHANGED)
        }

        private fun rePrepareNextSong() {
            controller.handler.removeMessages(RE_PREPARE_NEXT_PLAYER)
            controller.handler.sendEmptyMessage(RE_PREPARE_NEXT_PLAYER)
        }
    }

    private fun initPlayerStateObserver(): PlayerStateObserver = object : PlayerStateObserver {
        override fun onPlayerStateChanged(oldState: PlayerState, newState: PlayerState) {
            notifyChange(PLAY_STATE_CHANGED)
        }

        override fun onReceivingMessage(msg: Int) {
            when (msg) {
                MSG_NOW_PLAYING_CHANGED -> notifyChange(META_CHANGED)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.action != null) {
                when (intent.action) {
                    ACTION_TOGGLE_PAUSE          -> if (isPlaying) pause() else play()
                    ACTION_PAUSE                 -> pause()
                    ACTION_PLAY                  -> play()
                    ACTION_REWIND                -> back(false)
                    ACTION_SKIP                  -> playNextSong(false)
                    ACTION_STOP_AND_QUIT_NOW     -> stopSelf()
                    ACTION_STOP_AND_QUIT_PENDING -> controller.quitAfterFinishCurrentSong = true
                    ACTION_CANCEL_PENDING_QUIT   -> controller.quitAfterFinishCurrentSong = false
                }
            }
        }
        return START_NOT_STICKY
    }

    val playerState get() = controller.playerState

    val isPlaying: Boolean get() = controller.isPlaying()

    var isDestroyed = false
        private set

    override fun onDestroy() {
        isDestroyed = true
        mediaSessionController.mediaSession.isActive = false
        closeAudioEffectSession()
        playNotificationManager.onDestroy(this)
        mediaSessionController.onDestroy(this)
        coverLoader.terminate()
        unregisterReceiver(widgetIntentReceiver)
        mediaStoreObserverUtil.unregisterMediaStoreObserver(this)
        controller.removeObserver(playerStateObserver)
        controller.onDestroy(this)
        queueManager.removeObserver(queueChangeObserver)
        queueManager.apply {
            // todo
            post(MSG_SAVE_QUEUE)
            post(MSG_SAVE_CFG)
        }
        coroutineScope.cancel()
        _coroutineScope = null
        sendBroadcast(Intent("player.phonograph.PHONOGRAPH_MUSIC_SERVICE_DESTROYED"))
        super.onDestroy()
    }

    private fun closeAudioEffectSession() {
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, controller.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
        )
    }

    fun playSongAt(position: Int) = controller.playAt(position)
    fun pause() = controller.pause(releaseResource = true, reason = PlayerController.PAUSE_BY_MANUAL_ACTION)
    fun play() = controller.play()
    fun playPreviousSong(force: Boolean) = controller.jumpBackward(force)
    fun back(force: Boolean) = controller.back(force)
    fun playNextSong(force: Boolean) = controller.jumpForward(force)
    val songProgressMillis: Int get() = controller.getSongProgressMillis()
    val songDurationMillis: Int get() = controller.getSongDurationMillis()
    var speed: Float
        get() = controller.playerSpeed()
        set(value) {
            controller.setPlayerSpeed(value)
        }

    fun seek(millis: Int): Int = synchronized(this) {
        return try {
            val newPosition = controller.seekTo(millis.toLong())
            throttledTimer.notifySeek()
            newPosition
        } catch (e: Exception) {
            -1
        }
    }

    val audioSessionId: Int get() = controller.audioSessionId
    val mediaSession get() = mediaSessionController.mediaSession

    private fun notifyChange(what: String) {
        handleAndSendChangeInternal(what)
        MusicServiceUtil.sendPublicIntent(this, what)
    }

    private fun handleAndSendChangeInternal(what: String) {
        handleChangeInternal(what)
        sendChangeInternal(what)
    }

    private fun sendChangeInternal(what: String) {
        sendBroadcast(Intent(what).apply { `package` = ACTUAL_PACKAGE_NAME })
        notifyWidget(what)
    }

    private fun handleChangeInternal(what: String) {
        when (what) {

            PLAY_STATE_CHANGED                        -> {
                // update playing notification
                playNotificationManager.updateNotification(queueManager.currentSong)
                mediaSessionController.updateMetaData(
                    queueManager.currentSong,
                    (queueManager.currentSongPosition + 1).toLong(),
                    queueManager.playingQueue.size.toLong(),
                    false
                )
                mediaSessionController.updatePlaybackState(
                    controller.isPlaying(), controller.getSongProgressMillis().toLong()
                )

                // save state
                if (!isPlaying && songProgressMillis > 0) {
                    controller.saveCurrentMills()
                }

                songPlayCountHelper.notifyPlayStateChanged(isPlaying)

                if (!playNotificationManager.persistent) {
                    // wait for seconds and try to stop foreground notification
                    throttledTimer.setCancelableNotificationTimer(5_000)
                }
            }

            REPEAT_MODE_CHANGED, SHUFFLE_MODE_CHANGED -> {
                // just update playing notification
                playNotificationManager.updateNotification(queueManager.currentSong)
                mediaSessionController.updateMetaData(
                    queueManager.currentSong,
                    (queueManager.currentSongPosition + 1).toLong(),
                    queueManager.playingQueue.size.toLong(),
                    false
                )
                mediaSessionController.updatePlaybackState(
                    controller.isPlaying(), controller.getSongProgressMillis().toLong()
                )
            }

            META_CHANGED                              -> {
                // update playing notification
                playNotificationManager.updateNotification(queueManager.currentSong)
                mediaSessionController.updateMetaData(
                    queueManager.currentSong,
                    (queueManager.currentSongPosition + 1).toLong(),
                    queueManager.playingQueue.size.toLong(),
                    true
                )
                mediaSessionController.updatePlaybackState(
                    controller.isPlaying(), controller.getSongProgressMillis().toLong()
                )

                // save state
                queueManager.post(MSG_SAVE_CFG)
                controller.saveCurrentMills()

                // add to history
                get<HistoryStore>().addSongId(queueManager.currentSong.id)

                // check for bumping
                songPlayCountHelper.checkForBumpingPlayCount(get()) // old
                songPlayCountHelper.songMonitored = queueManager.currentSong // new
            }

            QUEUE_CHANGED                             -> {
                // update playing notification
                mediaSessionController.updateMetaData(
                    queueManager.currentSong,
                    (queueManager.currentSongPosition + 1).toLong(),
                    queueManager.playingQueue.size.toLong(),
                    true
                )
                // because playing queue size might have changed

                // save state
                queueManager.post(MSG_SAVE_QUEUE)
                queueManager.post(MSG_SAVE_CFG)

                // notify controller
                if (queueManager.playingQueue.isNotEmpty()) {
                    controller.handler.removeMessages(
                        RE_PREPARE_NEXT_PLAYER
                    )
                    controller.handler.sendEmptyMessage(
                        RE_PREPARE_NEXT_PLAYER
                    )
                } else {
                    controller.stop()
                    playNotificationManager.cancelNotification()
                }
            }
        }
    }

    private val widgetIntentReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val command = intent.getStringExtra(EXTRA_APP_WIDGET_NAME)
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                when (command) {
                    AppWidgetClassic.NAME -> {
                        AppWidgetClassic.instance.performUpdate(this@MusicService, ids)
                    }

                    AppWidgetSmall.NAME   -> {
                        AppWidgetSmall.instance.performUpdate(this@MusicService, ids)
                    }

                    AppWidgetBig.NAME     -> {
                        AppWidgetBig.instance.performUpdate(this@MusicService, ids)
                    }

                    AppWidgetCard.NAME    -> {
                        AppWidgetCard.instance.performUpdate(this@MusicService, ids)
                    }
                }
            }
        }

    private fun notifyWidget(what: String) {
        AppWidgetBig.instance.notifyChange(this, what)
        AppWidgetClassic.instance.notifyChange(this, what)
        AppWidgetSmall.instance.notifyChange(this, what)
        AppWidgetCard.instance.notifyChange(this, what)
    }


    private fun observeSettings() {
        val setting = Setting(this)
        fun <T> collect(key: PrimitiveKey<T>, collector: FlowCollector<T>) {
            coroutineScope.launch(SupervisorJob()) {
                setting[key].flow.distinctUntilChanged().collect(collector)
            }
        }
        collect(Keys.broadcastCurrentPlayerState) { broadcastCurrentPlayerState ->
            throttledTimer.broadcastCurrentPlayerState = broadcastCurrentPlayerState
        }
    }

    fun replaceLyrics(lyrics: LrcLyrics?) = controller.replaceLyrics(lyrics)

    private inner class ThrottledTimer(private val mHandler: Handler) : Runnable {
        var broadcastCurrentPlayerState: Boolean = false
        fun notifySeek() {
            mediaSessionController.updateMetaData(
                queueManager.currentSong,
                (queueManager.currentSongPosition + 1).toLong(),
                queueManager.playingQueue.size.toLong(),
                false
            )
            mediaSessionController.updatePlaybackState(
                controller.isPlaying(), controller.getSongProgressMillis().toLong()
            )
            mHandler.removeCallbacks(this)
            mHandler.postDelayed(this, THROTTLE)
        }

        private val onSetCancelableNotification = Runnable {
            if (controller.playerState != PlayerState.PLAYING) {
                when (controller.pauseReason) {
                    PlayerController.PAUSE_BY_MANUAL_ACTION, PlayerController.PAUSE_FOR_QUEUE_ENDED, PlayerController.PAUSE_ERROR,
                    -> stopForeground(STOP_FOREGROUND_DETACH)
                }
            }
        }

        fun setCancelableNotificationTimer(time: Long) {
            mHandler.removeCallbacks(onSetCancelableNotification)
            mHandler.postDelayed(onSetCancelableNotification, time)
        }

        override fun run() {
            controller.saveCurrentMills()
            if (broadcastCurrentPlayerState) {
                MusicServiceUtil.sendPublicIntent(this@MusicService, PLAY_STATE_CHANGED) // for musixmatch synced lyrics
            }
        }
    }

    internal fun requireRefreshMediaSessionState() {
        mediaSessionController.updatePlaybackState(
            controller.isPlaying(), controller.getSongProgressMillis().toLong()
        )
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        log("onGetRoot() clientPackageName: $clientPackageName, clientUid: $clientUid", false)
        log("onGetRoot() rootHints: ${rootHints?.toString()}", false)
        return try {
            MediaBrowserDelegate.onGetRoot(this, clientPackageName, clientUid, rootHints)
        } catch (e: Throwable) {
            recordThrowable(this, javaClass.name, e)
            null
        }
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        log("onLoadChildren(): parentId $parentId", false)
        runBlocking {
            val context = this@MusicService
            val mediaItems = try {
                MediaBrowserDelegate.listChildren(parentId, context)
            } catch (e: Throwable) {
                recordThrowable(context, javaClass.name, e)
                MediaBrowserDelegate.error(context)
            }
            result.sendResult(ArrayList(mediaItems))
        }
    }


    override fun attachBaseContext(base: Context?) {
        // Localization
        super.attachBaseContext(
            ContextLocaleDelegate.attachBaseContext(base)
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // Localization
        super.onConfigurationChanged(
            ContextLocaleDelegate.onConfigurationChanged(this, newConfig)
        )
    }

    @Suppress("SpellCheckingInspection")
    companion object {
        const val ACTION_TOGGLE_PAUSE = "$ACTUAL_PACKAGE_NAME.togglepause"
        const val ACTION_PLAY = "$ACTUAL_PACKAGE_NAME.play"
        const val ACTION_PAUSE = "$ACTUAL_PACKAGE_NAME.pause"
        const val ACTION_SKIP = "$ACTUAL_PACKAGE_NAME.skip"
        const val ACTION_REWIND = "$ACTUAL_PACKAGE_NAME.rewind"
        const val ACTION_STOP_AND_QUIT_NOW = "$ACTUAL_PACKAGE_NAME.stop_and_quit_now"
        const val ACTION_STOP_AND_QUIT_PENDING = "$ACTUAL_PACKAGE_NAME.stop_and_quit_pending"
        const val ACTION_CANCEL_PENDING_QUIT = "$ACTUAL_PACKAGE_NAME.cancel_pending_quit"

        const val MEDIA_SESSION_ACTION_TOGGLE_SHUFFLE = "$ACTUAL_PACKAGE_NAME.toggle_shuffle"
        const val MEDIA_SESSION_ACTION_TOGGLE_REPEAT = "$ACTUAL_PACKAGE_NAME.toggle_repeat"

        const val APP_WIDGET_UPDATE = "$ACTUAL_PACKAGE_NAME.appwidgetupdate"
        const val EXTRA_APP_WIDGET_NAME = ACTUAL_PACKAGE_NAME + "app_widget_name"

        private const val THROTTLE: Long = 500

        fun log(msg: String, force: Boolean) {
            if (force || BuildConfig.DEBUG) Log.i("MusicServiceDebug", msg)
        }

    }

    override fun onBind(intent: Intent): IBinder {
        return if (SERVICE_INTERFACE == intent.action) {
            log("onBind(): bind to $SERVICE_INTERFACE", true)
            super.onBind(intent) ?: musicBind
        } else {
            log("onBind(): bind to common MusicBinder", true)
            musicBind
        }
    }

    private val musicBind: IBinder = MusicBinder()

    inner class MusicBinder : Binder() {
        val service: MusicService get() = this@MusicService
    }
}
