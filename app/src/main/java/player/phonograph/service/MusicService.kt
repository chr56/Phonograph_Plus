/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.service

import lib.phonograph.localization.ContextLocaleDelegate
import org.koin.android.ext.android.get
import player.phonograph.ACTUAL_PACKAGE_NAME
import player.phonograph.BuildConfig
import player.phonograph.appwidgets.AppWidgetUpdateReceiver
import player.phonograph.mechanism.broadcast.setUpMediaStoreObserver
import player.phonograph.mechanism.broadcast.unregisterMediaStoreObserver
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.service.ACTION_CANCEL_PENDING_QUIT
import player.phonograph.model.service.ACTION_CONNECT_WIDGETS
import player.phonograph.model.service.ACTION_EXIT_OR_STOP
import player.phonograph.model.service.ACTION_FAST_FORWARD
import player.phonograph.model.service.ACTION_FAST_REWIND
import player.phonograph.model.service.ACTION_FAV
import player.phonograph.model.service.ACTION_NEXT
import player.phonograph.model.service.ACTION_PAUSE
import player.phonograph.model.service.ACTION_PLAY
import player.phonograph.model.service.ACTION_PREVIOUS
import player.phonograph.model.service.ACTION_REPEAT
import player.phonograph.model.service.ACTION_SHUFFLE
import player.phonograph.model.service.ACTION_STOP_AND_QUIT_NOW
import player.phonograph.model.service.ACTION_STOP_AND_QUIT_PENDING
import player.phonograph.model.service.ACTION_TOGGLE_PAUSE
import player.phonograph.model.service.EVENT_META_CHANGED
import player.phonograph.model.service.EVENT_PLAY_STATE_CHANGED
import player.phonograph.model.service.EVENT_QUEUE_CHANGED
import player.phonograph.model.service.EVENT_REPEAT_MODE_CHANGED
import player.phonograph.model.service.EVENT_SHUFFLE_MODE_CHANGED
import player.phonograph.model.service.MusicServiceStatus
import player.phonograph.model.service.PlayerState
import player.phonograph.model.service.PlayerStateObserver
import player.phonograph.model.service.QueueObserver
import player.phonograph.model.service.RepeatMode
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.browser.MediaBrowserDelegate
import player.phonograph.repo.database.store.HistoryStore
import player.phonograph.repo.loader.FavoriteSongs
import player.phonograph.service.notification.CoverLoader
import player.phonograph.service.notification.PlayingNotificationManager
import player.phonograph.service.notification.PlayingNotificationManager.Companion.VERSION_SET_COVER_USING_METADATA
import player.phonograph.service.player.MediaSessionController
import player.phonograph.service.player.PauseReason
import player.phonograph.service.player.PlayerController
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.QueueManager.Companion.MSG_SAVE_CFG
import player.phonograph.service.queue.QueueManager.Companion.MSG_SAVE_QUEUE
import player.phonograph.service.util.MusicServiceUtil
import player.phonograph.service.util.SongPlayCountHelper
import player.phonograph.settings.Keys
import player.phonograph.settings.SettingObserver
import player.phonograph.util.recordThrowable
import androidx.media.MediaBrowserServiceCompat
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.audiofx.AudioEffect
import android.os.Binder
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
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

    private val playNotificationManager: PlayingNotificationManager = PlayingNotificationManager()

    private val mediaSessionController: MediaSessionController = MediaSessionController()

    private lateinit var throttledTimer: ThrottledTimer

    lateinit var coverLoader: CoverLoader

    val coroutineScope get() = _coroutineScope!!
    private var _coroutineScope: CoroutineScope? = null

    override fun onCreate() {

        _coroutineScope = CoroutineScope(Dispatchers.IO)
        super.onCreate()

        // controller
        controller.onCreate(this)

        // observers & messages
        sendChangeInternal(EVENT_META_CHANGED) // notify manually for first setting up queueManager
        sendChangeInternal(EVENT_QUEUE_CHANGED) // notify manually for first setting up queueManager
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

        // setting
        val settingObserver = SettingObserver(this, coroutineScope)
        settingObserver.collect(Keys.broadcastCurrentPlayerState) { broadcastCurrentPlayerState ->
            throttledTimer.broadcastCurrentPlayerState = broadcastCurrentPlayerState
        }
        settingObserver.collect(Keys.alwaysUseMediaSessionToDisplayCover) { alwaysUseMediaSessionToDisplayCover ->
            couldPutCover = SDK_INT >= VERSION_SET_COVER_USING_METADATA || alwaysUseMediaSessionToDisplayCover
        }
        // misc
        setUpMediaStoreObserver(
            this,
            controller.handler, // todo use other handler
            this@MusicService::handleAndSendChangeInternal
        )
        AppWidgetUpdateReceiver.register(this)
        sendBroadcast(Intent("player.phonograph.PHONOGRAPH_MUSIC_SERVICE_CREATED"))
    }

    private fun initQueueChangeObserver(): QueueObserver = object : QueueObserver {
        override fun onCurrentPositionChanged(newPosition: Int) {
            notifyChange(EVENT_META_CHANGED)
            rePrepareNextSong()
        }

        override fun onCurrentSongChanged(newSong: Song?) {
            notifyChange(EVENT_META_CHANGED)
            rePrepareNextSong()
        }

        override fun onQueueChanged(newPlayingQueue: List<Song>) {
            handleAndSendChangeInternal(EVENT_QUEUE_CHANGED)
            notifyChange(EVENT_META_CHANGED)
            rePrepareNextSong()
        }

        override fun onShuffleModeChanged(newMode: ShuffleMode) {
            rePrepareNextSong()
            handleAndSendChangeInternal(EVENT_SHUFFLE_MODE_CHANGED)
        }

        override fun onRepeatModeChanged(newMode: RepeatMode) {
            rePrepareNextSong()
            handleAndSendChangeInternal(EVENT_REPEAT_MODE_CHANGED)
        }

        private fun rePrepareNextSong() {
            controller.prepareNext()
        }
    }

    private fun initPlayerStateObserver(): PlayerStateObserver = object : PlayerStateObserver {
        override fun onPlayerStateChanged(oldState: PlayerState, newState: PlayerState) {
            notifyChange(EVENT_PLAY_STATE_CHANGED)
        }

        override fun onReceivingMessage(msg: Int) {
            when (msg) {
                PlayerStateObserver.MSG_NOW_PLAYING_CHANGED -> notifyChange(EVENT_META_CHANGED)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.action != null) processCommand(intent.action)
        }
        return START_NOT_STICKY
    }

    fun processCommand(action: String?) {
        when (action) {
            ACTION_TOGGLE_PAUSE          -> if (isPlaying) pause() else play()
            ACTION_PLAY                  -> play()
            ACTION_PAUSE                 -> pause()
            ACTION_NEXT                  -> playNextSong(true)
            ACTION_PREVIOUS              -> back(true)
            ACTION_FAST_FORWARD          -> fastForward()
            ACTION_FAST_REWIND           -> fastRewind()
            ACTION_SHUFFLE               -> queueManager.modifyShuffleMode(null)
            ACTION_REPEAT                -> queueManager.modifyRepeatMode(null)
            ACTION_FAV                   -> toggleFavorite(queueManager.currentSong)
            ACTION_EXIT_OR_STOP          -> exitOrStop()
            ACTION_STOP_AND_QUIT_NOW     -> exitOrStop()
            ACTION_STOP_AND_QUIT_PENDING -> controller.quitAfterFinishCurrentSong = true
            ACTION_CANCEL_PENDING_QUIT   -> controller.quitAfterFinishCurrentSong = false
            ACTION_CONNECT_WIDGETS       -> AppWidgetUpdateReceiver.connect(this)
        }
    }

    private fun exitOrStop() {
        log("serviceUsedInForeground: $serviceUsedInForeground", false)
        if (serviceUsedInForeground > 0) {
            pause()
            MusicPlayerRemote.unbindAllFromService()
            log("All service bindings are removed", false)
            controller.handler.post {
                stopSelf()
            }
        } else {
            stopSelf()
        }
    }


    private fun toggleFavorite(song: Song?): Boolean {
        return if (song != null) runBlocking { FavoriteSongs.toggleFavorite(this@MusicService, song) } else false
    }


    fun addPlayerStateObserver(observer: PlayerStateObserver) {
        controller.addObserver(observer)
    }

    fun removePlayerStateObserver(observer: PlayerStateObserver) {
        controller.removeObserver(observer)
    }

    val playerState get() = controller.playerState

    val isPlaying: Boolean get() = controller.isPlaying

    var isDestroyed = false
        private set

    override fun onDestroy() {
        isDestroyed = true
        mediaSessionController.mediaSession.isActive = false
        closeAudioEffectSession()
        playNotificationManager.onDestroy(this)
        mediaSessionController.onDestroy(this)
        coverLoader.terminate()
        AppWidgetUpdateReceiver.unRegister(this)
        unregisterMediaStoreObserver(this)
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
    fun pause() = controller.pause(releaseResource = true, reason = PauseReason.PAUSE_BY_MANUAL_ACTION)
    fun play() = controller.play()
    fun playPreviousSong(force: Boolean) = controller.jumpBackward(force)
    fun back(force: Boolean) = controller.back(force)
    fun playNextSong(force: Boolean) = controller.jumpForward(force)
    val songProgressMillis: Int get() = controller.songProgressMillis
    val songDurationMillis: Int get() = controller.songDurationMillis
    var speed: Float by controller::playerSpeed

    fun seek(targetMilli: Int): Boolean = try {
        if (targetMilli in 0..controller.songDurationMillis) {
            controller.seekTo(targetMilli.toLong())
            throttledTimer.notifySeek()
            true
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }

    private fun fastForward(millis: Int = 10_000) = seek(millis)
    private fun fastRewind(millis: Int = 10_000) = seek(-millis)

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
    }

    private fun handleChangeInternal(what: String) {
        when (what) {

            EVENT_PLAY_STATE_CHANGED -> {
                // update playing notification & widgets
                updateNotificationAndMediaSession()
                AppWidgetUpdateReceiver.notifyWidgets(this, isPlaying)

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

            EVENT_REPEAT_MODE_CHANGED, EVENT_SHUFFLE_MODE_CHANGED -> {
                // just update playing notification & widgets
                updateNotificationAndMediaSession()
                AppWidgetUpdateReceiver.notifyWidgets(this, isPlaying)
            }

            EVENT_META_CHANGED -> {
                // update playing notification & widgets
                updateNotificationAndMediaSession()
                AppWidgetUpdateReceiver.notifyWidgets(this, isPlaying)

                val currentSong = queueManager.currentSong
                if (currentSong != null) {
                    // save state
                    queueManager.post(MSG_SAVE_CFG)
                    controller.saveCurrentMills()

                    // add to history
                    get<HistoryStore>().addSongId(currentSong.id)

                    // check for bumping
                    songPlayCountHelper.checkForBumpingPlayCount(get()) // old
                    songPlayCountHelper.songMonitored = queueManager.currentSong // new
                }
            }

            EVENT_QUEUE_CHANGED -> {
                // update playing notification
                mediaSessionController.updateMetaData(
                    queueManager.currentSong,
                    (queueManager.currentSongPosition + 1).toLong(),
                    queueManager.playingQueue.size.toLong(),
                    couldPutCover
                )
                // because playing queue size might have changed

                // save state
                queueManager.post(MSG_SAVE_QUEUE)
                queueManager.post(MSG_SAVE_CFG)

                // notify controller
                if (queueManager.playingQueue.isNotEmpty()) {
                    controller.prepareNext()
                } else {
                    controller.stop()
                    playNotificationManager.cancelNotification()
                }
            }
        }
    }

    private fun updateNotificationAndMediaSession() {
        val currentSong = queueManager.currentSong
        val serviceStatus = statusForNotification
        playNotificationManager.updateNotification(currentSong, serviceStatus)
        mediaSessionController.updateMetaData(
            currentSong,
            (queueManager.currentSongPosition + 1).toLong(),
            queueManager.playingQueue.size.toLong(),
            couldPutCover
        )
        mediaSessionController.updatePlaybackState(serviceStatus)
    }

    private var couldPutCover: Boolean = true

    fun replaceLyrics(lyrics: LrcLyrics?) = controller.replaceLyrics(lyrics)

    private inner class ThrottledTimer(private val mHandler: Handler) : Runnable {
        var broadcastCurrentPlayerState: Boolean = false
        fun notifySeek() {
            mediaSessionController.updateMetaData(
                queueManager.currentSong,
                (queueManager.currentSongPosition + 1).toLong(),
                queueManager.playingQueue.size.toLong(),
                couldPutCover
            )
            mediaSessionController.updatePlaybackState(statusForNotification)
            mHandler.removeCallbacks(this)
            mHandler.postDelayed(this, THROTTLE)
        }

        private val onSetCancelableNotification = Runnable {
            if (controller.playerState != PlayerState.PLAYING) {
                when (controller.pauseReason) {
                    PauseReason.PAUSE_BY_MANUAL_ACTION, PauseReason.PAUSE_FOR_QUEUE_ENDED, PauseReason.PAUSE_ERROR,
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
                MusicServiceUtil.sendPublicIntent(this@MusicService, EVENT_PLAY_STATE_CHANGED) // for musixmatch synced lyrics
            }
        }
    }

    val statusForNotification: MusicServiceStatus
        get() = MusicServiceStatus(
            isPlaying,
            queueManager.shuffleMode,
            queueManager.repeatMode
        )

    internal fun requireRefreshMediaSessionState() {
        mediaSessionController.updatePlaybackState(statusForNotification)
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

    companion object {
        private const val THROTTLE: Long = 500

        fun log(msg: String, force: Boolean) {
            if (force || BuildConfig.DEBUG) Log.i("MusicServiceDebug", msg)
        }

    }

    private var serviceUsedInForeground: Int = 0
    override fun onBind(intent: Intent): IBinder {
        serviceUsedInForeground++
        return if (SERVICE_INTERFACE == intent.action) {
            log("onBind(): bind to $SERVICE_INTERFACE", true)
            super.onBind(intent) ?: musicBind
        } else {
            log("onBind(): bind to common MusicBinder", true)
            musicBind
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        serviceUsedInForeground--
        log("onUnbind()", true)
        return super.onUnbind(intent)
    }

    private val musicBind: IBinder = MusicBinder()

    inner class MusicBinder : Binder() {
        val service: MusicService get() = this@MusicService
    }
}
