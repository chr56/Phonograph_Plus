/*
 *  Copyright (c) 2021~2026 chr_56, Karim Abou Zeid (kabouzeid), Andrew Neal
 */

package player.phonograph.service

import org.koin.android.ext.android.get
import player.phonograph.ACTUAL_PACKAGE_NAME
import player.phonograph.BuildConfig
import player.phonograph.foundation.error.warning
import player.phonograph.foundation.localization.ContextLocaleDelegate
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.service.*
import player.phonograph.repo.database.domain.DynamicTracks
import player.phonograph.repo.loader.FavoriteTracks
import player.phonograph.service.notification.CoverLoader
import player.phonograph.service.notification.PlayingNotificationManager
import player.phonograph.service.notification.PlayingNotificationManager.Companion.VERSION_SET_COVER_USING_METADATA
import player.phonograph.service.player.MediaSessionController
import player.phonograph.service.player.PauseReason
import player.phonograph.service.player.PlayerController
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.QueueManager.Companion.MSG_SAVE_CFG
import player.phonograph.service.queue.QueueManager.Companion.MSG_SAVE_QUEUE
import player.phonograph.service.util.AppWidgetUpdateReceiver
import player.phonograph.service.util.SongPlayCountHelper
import player.phonograph.settings.Keys
import player.phonograph.settings.SettingsObserver
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Binder
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MusicService :
        MediaLibraryService(),
        QueueObserver, PlayerStateObserver {

    val queueManager: QueueManager = get()

    val controller: PlayerController = PlayerController()

    private val playNotificationManager: PlayingNotificationManager = PlayingNotificationManager()

    private val mediaSessionController: MediaSessionController = MediaSessionController()

    private lateinit var throttledTimer: ThrottledTimer
    private val mainHandler = Handler(Looper.getMainLooper())
    private var quitting = false

    private var _coroutineScope: CoroutineScope? = null
    val coroutineScope get() = _coroutineScope!!

    lateinit var coverLoader: CoverLoader

    var isDestroyed = false
        private set

    //////////////////

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

    override fun onCreate() {

        _coroutineScope = CoroutineScope(Dispatchers.IO)
        super.onCreate()

        // controller
        controller.onCreate(this)

        // observers & messages
        sendInternalIntent(EVENT_META_CHANGED) // notify manually for first setting up queueManager
        sendInternalIntent(EVENT_QUEUE_CHANGED) // notify manually for first setting up queueManager

        // notifications & media session
        coverLoader = CoverLoader(this)
        mediaSessionController.onCreate(this)
        playNotificationManager.onCreate(this)

        // process updater
        throttledTimer = ThrottledTimer(controller.handler)
        queueManager.addObserver(this)
        controller.addObserver(this)
        updateNotificationAndMediaSession()

        // settings
        observeSettings()
        // misc
        AppWidgetUpdateReceiver.register(this)
        sendBroadcast(Intent("player.phonograph.PHONOGRAPH_MUSIC_SERVICE_CREATED"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.action != null) processCommand(intent.action)
        }
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    fun processCommand(action: String?) {
        if (quitting || isDestroyed) return
        when (action) {
            ACTION_TOGGLE_PAUSE         -> if (isPlaying) pause() else play()
            ACTION_PLAY                 -> play()
            ACTION_PAUSE                -> pause()
            ACTION_NEXT                 -> playNextSong(true)
            ACTION_PREVIOUS             -> back(true)
            ACTION_FAST_FORWARD         -> fastForward()
            ACTION_FAST_REWIND          -> fastRewind()
            ACTION_SHUFFLE              -> queueManager.modifyShuffleMode(alongWithQueue = queueManager.playingQueue.isNotEmpty())
            ACTION_REPEAT               -> queueManager.modifyRepeatMode(null)
            ACTION_FAV                  -> toggleFavorite(queueManager.currentSong)
            ACTION_QUIT_BY_USER_ACTION  -> requestToQuit()
            ACTION_QUIT_BY_NOTIFICATION -> requestToQuit()
            ACTION_CONNECT_WIDGETS      -> AppWidgetUpdateReceiver.connect(this)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (stopOnTaskRemoved || !isPlaying) requestToQuit()
    }

    override fun onDestroy() {
        isDestroyed = true
        cancelSettingsObservation()
        throttledTimer.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        if (playNotificationManager.created) playNotificationManager.onDestroy(this)
        if (mediaSessionController.created) mediaSessionController.onDestroy(this)
        coverLoader.terminate()
        AppWidgetUpdateReceiver.unRegister(this)
        controller.removeObserver(this)
        controller.onDestroy(this)
        queueManager.removeObserver(this)
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


    //////////////////

    //region Settings

    private var broadcastCurrentPlayerState: Boolean = false

    private var couldPutCover: Boolean = true

    internal var seekJumpInterval: Int = 5_000
        private set

    private var stopOnTaskRemoved: Boolean = false

    private var enableHistory: Boolean = false

    private val songPlayCountHelper = SongPlayCountHelper()

    private val _jobs = mutableListOf<Job>()

    private fun observeSettings() {
        val settingsObserver = SettingsObserver(this, coroutineScope)
        _jobs += settingsObserver.collect(Keys.broadcastCurrentPlayerState) { value ->
            broadcastCurrentPlayerState = value
        }
        _jobs += settingsObserver.collect(Keys.alwaysUseMediaSessionToDisplayCover) { alwaysUseMediaSessionToDisplayCover ->
            couldPutCover = SDK_INT >= VERSION_SET_COVER_USING_METADATA || alwaysUseMediaSessionToDisplayCover
            mediaSessionController.updateMetaData(queueManager.currentSong, couldPutCover)
        }
        _jobs += settingsObserver.collect(Keys.seekJumpInterval) { interval ->
            seekJumpInterval = (interval * 1000).toInt().coerceAtLeast(1)
            requireRefreshMediaSessionState()
        }
        _jobs += settingsObserver.collect(Keys.stopOnTaskRemoved) { value ->
            stopOnTaskRemoved = value
        }
        _jobs += settingsObserver.collect(Keys.enableHistory) { value ->
            enableHistory = value
        }
        _jobs += settingsObserver.collect(Keys.enableLyrics) { value ->
            controller.enableLyrics = value
        }
    }

    private fun cancelSettingsObservation() {
        for (job in _jobs) job.cancel()
        _jobs.clear()
    }
    //endregion

    fun addPlayerStateObserver(observer: PlayerStateObserver) {
        controller.addObserver(observer)
    }

    fun removePlayerStateObserver(observer: PlayerStateObserver) {
        controller.removeObserver(observer)
    }


    //////////////////

    val audioSessionId: Int get() = controller.audioSessionId
    val mediaSession get() = mediaSessionController.mediaSession

    val playerState get() = controller.playerState

    val isPlaying: Boolean get() = controller.isPlaying

    val statusForNotification: MusicServiceStatus
        get() = MusicServiceStatus(
            isPlaying,
            queueManager.shuffleMode,
            queueManager.repeatMode
        )

    val songProgressMillis: Int get() = controller.songProgressMillis
    val songDurationMillis: Int get() = controller.songDurationMillis

    fun play() = controller.play()
    fun pause() = controller.pause(releaseResource = true, reason = PauseReason.PAUSE_BY_MANUAL_ACTION)
    fun stop() = controller.stop()

    fun playAt(position: Int) = controller.playAt(position)

    fun playPreviousSong(force: Boolean) = controller.jumpBackward(force)
    fun back(force: Boolean) = controller.back(force)
    fun playNextSong(force: Boolean) = controller.jumpForward(force)

    fun seek(targetMilli: Int): Boolean = try {
        if (targetMilli in 0..controller.songDurationMillis) {
            controller.seekTo(targetMilli.toLong())
            throttledTimer.notifySeek()
            true
        } else {
            false
        }
    } catch (e: Exception) {
        warning(this, "MusicService", "Failed to seek to $targetMilli", e)
        false
    }

    fun fastForward(millis: Int = seekJumpInterval) = seek(songProgressMillis + millis)
    fun fastRewind(millis: Int = seekJumpInterval) = seek(songProgressMillis - millis)

    val speed: Float get() = controller.playerSpeed
    fun setSpeed(speed: Float): Unit = controller.setPlayerSpeed(speed)

    fun requestToQuit() {
        if (quitting || isDestroyed) return
        quitting = true
        controller.quitRequestPending.set(true)
        controller.stop()
        log("serviceUsedInForeground: $serviceUsedInForeground", false)
        if (serviceUsedInForeground > 0) {
            MusicPlayerRemote.unbindAllFromService()
            log("All service bindings are removed", false)
        }
    }

    fun doQuit() {
        controller.quitRequestPending.set(false)
        mainHandler.post {
            if (!isDestroyed && mediaSessionController.created) {
                controller.removeObserver(this)
                queueManager.removeObserver(this)
                throttledTimer.cancel()
                playNotificationManager.onDestroy(this)
                mediaSessionController.onDestroy(this)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    fun setSleepTimer(minutesToQuit: Long, shouldFinishLastSong: Boolean): Boolean =
        controller.setSleepTimer(minutesToQuit, shouldFinishLastSong)

    fun cancelSleepTimer(): Boolean = controller.cancelSleepTimer()
    fun hasSleepTimer(): Boolean = controller.hasSleepTimer()
    fun sleepTimerElapsedTime(): Long = controller.sleepTimerElapsedTime()

    fun replaceLyrics(lyrics: LrcLyrics?) = controller.replaceLyrics(lyrics)

    fun toggleFavorite(song: Song?) {
        if (song != null) coroutineScope.launch {
            FavoriteTracks.toggleState(this@MusicService, song)
        }
    }

    //////////////////

    private fun updateNotificationAndMediaSession() {
        val currentSong = queueManager.currentSong
        val serviceStatus = statusForNotification
        playNotificationManager.updateNotification(currentSong, serviceStatus)
        mediaSessionController.updateMetaData(currentSong, couldPutCover)
        mediaSessionController.updatePlaybackState(serviceStatus)
    }

    private fun onCurrentPlayingChanged() {
        // update playing notification & widgets
        updateNotificationAndMediaSession()
        AppWidgetUpdateReceiver.notifyWidgets(this, isPlaying)

        val currentSong = queueManager.currentSong
        if (currentSong != null) {
            // save state
            queueManager.post(MSG_SAVE_CFG)
            controller.saveCurrentMills()

            // add to history
            if (enableHistory) coroutineScope.launch(Dispatchers.IO) {
                DynamicTracks.RecentTracks.add(currentSong.id)
            }

            // check for bumping
            if (enableHistory) songPlayCountHelper.checkForBumpingPlayCount(coroutineScope) // old
            songPlayCountHelper.songMonitored = queueManager.currentSong // new
        }

        sendInternalIntent(EVENT_META_CHANGED)
        sendPublicIntent(EVENT_META_CHANGED)
    }

    override fun onPlayerStateChanged(oldState: PlayerState, newState: PlayerState) {
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

        sendInternalIntent(EVENT_PLAY_STATE_CHANGED)
        sendPublicIntent(EVENT_PLAY_STATE_CHANGED)
    }

    override fun onCurrentPositionChanged(newPosition: Int) {
        onCurrentPlayingChanged()
        controller.prepareNextTrack()
    }

    override fun onCurrentSongChanged(newSong: Song?) {
        onCurrentPlayingChanged()
        controller.prepareNextTrack()
    }

    override fun onQueueChanged(newPlayingQueue: List<Song>) {
        // update playing notification
        mediaSessionController.notifyQueueChanged()

        // save state
        queueManager.post(MSG_SAVE_QUEUE)
        queueManager.post(MSG_SAVE_CFG)

        // notify controller
        if (queueManager.playingQueue.isNotEmpty()) {
            controller.prepareNextTrack()
        } else {
            controller.stop()
            playNotificationManager.cancelNotification()
        }

        sendInternalIntent(EVENT_QUEUE_CHANGED)
        onCurrentPlayingChanged()
        controller.prepareNextTrack()
    }

    override fun onShuffleModeChanged(newMode: ShuffleMode) {
        // update playing notification & widgets
        updateNotificationAndMediaSession()
        AppWidgetUpdateReceiver.notifyWidgets(this, isPlaying)

        sendInternalIntent(EVENT_SHUFFLE_MODE_CHANGED)
    }

    override fun onRepeatModeChanged(newMode: RepeatMode) {
        // update playing notification & widgets
        updateNotificationAndMediaSession()
        AppWidgetUpdateReceiver.notifyWidgets(this, isPlaying)

        sendInternalIntent(EVENT_REPEAT_MODE_CHANGED)
        controller.prepareNextTrack()
    }

    //////////////////

    fun requireRefreshMediaSessionState() {
        mediaSessionController.updatePlaybackState(statusForNotification)
    }

    private fun sendInternalIntent(what: String) {
        sendBroadcast(Intent(what).apply { `package` = ACTUAL_PACKAGE_NAME })
    }

    private fun sendPublicIntent(what: String) {
        if (broadcastCurrentPlayerState) {
            // for services likes MusixMatch etc.
            sendPublicIntent(
                this, what, queueManager.currentSong, isPlaying, songProgressMillis.toLong()
            )
        }
    }

    //region Media3 service
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        if (mediaSessionController.created && !isDestroyed && !quitting) mediaSession else null

    // Notifications and foreground lifetime remain owned by PlayingNotificationManager.
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        if (!isDestroyed && playNotificationManager.created && mediaSessionController.created) {
            playNotificationManager.updateNotification(queueManager.currentSong, statusForNotification)
        }
    }
    //endregion

    //region ThrottledTimer
    private inner class ThrottledTimer(private val mHandler: Handler) : Runnable {
        fun cancel() {
            mHandler.removeCallbacks(this)
            mHandler.removeCallbacks(onSetCancelableNotification)
        }

        fun notifySeek() {
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
            this@MusicService.sendPublicIntent(EVENT_PLAY_STATE_CHANGED)
        }
    }
    //endregion

    //region Binding
    private var serviceUsedInForeground: Int = 0
    override fun onBind(intent: Intent?): IBinder? {
        if (intent == null) return super.onBind(null)
        serviceUsedInForeground++
        return if (intent.action == SERVICE_INTERFACE || intent.action == MediaSessionService.SERVICE_INTERFACE ||
            intent.action == "android.media.browse.MediaBrowserService") {
            super.onBind(intent)
        } else {
            super.onBind(intent)
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
    //endregion

    companion object {
        private const val TAG = "MusicService"
        private const val THROTTLE: Long = 500

        private fun log(msg: String, force: Boolean = false) {
            if (force || BuildConfig.DEBUG) Log.i(TAG, msg)
        }

        private fun sendPublicIntent(
            context: Context,
            action: String,
            song: Song?,
            isPlaying: Boolean,
            progressMillis: Long,
        ) {
            if (song == null) return
            @Suppress("DEPRECATION")
            context.sendStickyBroadcast(
                Intent(action.replace(ACTUAL_PACKAGE_NAME, "com.android.music")).apply {
                    putExtra("id", song.id)
                    putExtra("artist", song.artistName)
                    putExtra("album", song.albumName)
                    putExtra("track", song.title)
                    putExtra("duration", song.duration)
                    putExtra("position", progressMillis)
                    putExtra("playing", isPlaying)
                    putExtra("scrobbling_source", ACTUAL_PACKAGE_NAME)
                }
            )
        }
    }
}
