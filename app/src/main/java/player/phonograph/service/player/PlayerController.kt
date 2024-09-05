/*
 *  Copyright (c) 2021~2026 chr_56 & Abou Zeid (kabouzeid)
 */

package player.phonograph.service.player

import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.foundation.mediastore.mediaStoreUriSongExternal
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.service.PlayerState
import player.phonograph.model.service.PlayerStateObserver
import player.phonograph.model.service.RepeatMode
import player.phonograph.service.MusicService
import player.phonograph.service.ServiceComponent
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.util.LyricsBroadcastController
import player.phonograph.service.util.QueuePreferenceManager
import player.phonograph.service.util.makeErrorMessage
import player.phonograph.settings.Keys
import player.phonograph.settings.SettingsObserver
import androidx.core.content.ContextCompat
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import kotlin.math.abs
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong


class PlayerController : ServiceComponent, Controller {

    private var _service: MusicService? = null
    val service: MusicService get() = _service!!

    private val queueManager: QueueManager get() = service.queueManager

    private var _impl: ControllerInternal? = null
    private val impl: ControllerInternal get() = _impl!!

    private var _wakeLock: WakeLock? = null
    private val wakeLock: WakeLock get() = _wakeLock!!

    private var _handler: ControllerHandler? = null
    val handler: ControllerHandler get() = _handler!!

    private var _thread: HandlerThread? = null
    private val thread: HandlerThread get() = _thread!!

    private var _lyricsBroadcastController: LyricsBroadcastController? = null
    private val lyricsBroadcastController: LyricsBroadcastController get() = _lyricsBroadcastController!!

    private var _audioFocusManager: AudioFocusManager? = null
    private val audioFocusManager: AudioFocusManager get() = _audioFocusManager!!

    override var created: Boolean = false

    override fun onCreate(musicService: MusicService) {
        _service = musicService

        // acquire resources
        _wakeLock =
            (service.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    javaClass.name
                ).apply {
                    setReferenceCounted(false)
                }
        _audioFocusManager = AudioFocusManager(this)

        // setup handler
        _thread = HandlerThread("player_controller_handler_thread")
        thread.start()
        _handler = ControllerHandler(this, thread.looper)

        // Prepare Player

        _impl = VanillaAudioPlayerControllerImpl(this)
        impl.onCreate(musicService)


        _lyricsBroadcastController = LyricsBroadcastController()
        updateLyrics()

        created = true

        observeSettings(musicService)
    }

    override fun onDestroy(musicService: MusicService) {
        created = false
        clearSleepTimer()

        impl.stop()
        impl.onDestroy(musicService)
        _impl = null

        audioFocusManager.abandonAudioFocus()
        releaseWakeLock()

        _wakeLock = null
        _audioFocusManager = null

        _lyricsBroadcastController = null

        thread.quitSafely()
        handler.looper.quitSafely()
        _thread = null
        _handler = null

        _service = null
    }


    private fun observeSettings(service: MusicService) {
        val settingsObserver = SettingsObserver(service, service.coroutineScope)
        settingsObserver.collect(Keys.audioDucking) { value ->
            audioDucking = value
        }
        settingsObserver.collect(Keys.resumeAfterAudioFocusGain) { value ->
            resumeAfterAudioFocusGain = value
        }
        settingsObserver.collect(Keys.alwaysPlay) { value ->
            ignoreAudioFocus = value
        }
        settingsObserver.collect(Keys.gaplessPlayback) { gaplessPlayback ->
            handler.post {
                val controllerImpl = _impl
                if (controllerImpl != null && controllerImpl.created) {
                    controllerImpl.gaplessPlayback = gaplessPlayback
                    if (gaplessPlayback) {
                        controllerImpl.prepareNextTrack()
                    } else {
                        controllerImpl.cancelNextTrackPreparation()
                    }
                }
            }
        }
        settingsObserver.collect(Keys.autoSkipUnplayable) { value ->
            shouldAutoSkipUnplayable = value
        }
        settingsObserver.collect(Keys.broadcastSynchronizedLyrics) { value ->
            broadcastSynchronizedLyrics = value
        }
    }

    /**
     * release taken resources but not vitals
     */
    private fun releaseTakenResources() {
        releaseWakeLock()
        audioFocusManager.abandonAudioFocus()
    }

    private fun acquireWakeLock(milli: Long) {
        wakeLock.acquire(milli)
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    var playerState: PlayerState = PlayerState.PREPARING
        private set(value) {
            synchronized(field) {
                val oldState = field
                field = value
                for (observer in observers) {
                    observer.onPlayerStateChanged(oldState, value)
                }
            }
        }


    interface ControllerInternal : Controller, ServiceComponent

    sealed class AudioPlayerControllerImpl(
        val controller: PlayerController,
    ) : ControllerInternal, Playback.PlaybackCallbacks {

        override var created: Boolean = false

        abstract fun initPlayer(musicService: MusicService): Playback

        private val queueManager: QueueManager get() = controller.queueManager

        private var _service: MusicService? = null
        val service: MusicService get() = _service!!

        private var _audioPlayer: Playback? = null
        private val audioPlayer: Playback get() = _audioPlayer!!

        private val handler: ControllerHandler get() = controller.handler
        private val audioFocusManager: AudioFocusManager get() = controller.audioFocusManager

        override fun onCreate(musicService: MusicService) {
            _service = musicService

            _audioPlayer = initPlayer(musicService)

            created = true
            restore(musicService)
        }

        override fun onDestroy(musicService: MusicService) {
            audioPlayer.stop(release = true)
            unregisterBecomingNoisyReceiver(musicService)
            created = false

            _audioPlayer = null

            _service = null
        }


        private fun restore(musicService: MusicService) {
            val currentSong = queueManager.currentSong ?: return
            handler.post {
                if (prepareCurrentPlayer(currentSong)) {
                    val restored = QueuePreferenceManager(musicService).currentMillisecond
                    if (restored > 0) seekTo(restored.toLong())
                }
            }
        }


        private fun sourceUri(song: Song?): String? =
            if (song != null && song.data.isNotEmpty()) {
                mediaStoreUriSongExternal(song.id).toString()
            } else {
                null
            }

        /**
         * prepare current player data source safely
         * @param song what to play now
         * @return true if success
         */
        private fun prepareCurrentPlayer(song: Song?): Boolean {
            val uri = sourceUri(song)
            return uri != null && audioPlayer.setDataSource(uri)
        }

        /**
         * prepare next player data source safely
         */
        override fun prepareNextTrack() {
            if (!audioPlayer.isInitialized) return
            audioPlayer.setNextDataSource(sourceUri(queueManager.nextSong))
        }
        /**
         * cancel the prepared next track
         */
        override fun cancelNextTrackPreparation() {
            if (!audioPlayer.isInitialized) return
            audioPlayer.setNextDataSource(null)
        }

        /**
         * prepare current and next player and set queue cursor(position)
         * @param position where to start in queue
         * @return true if it is ready
         */
        private fun prepareSongs(position: Int): Boolean {
            if (position < 0) {
                log("prepareSongs", "illegal position $position")
                return false
            }
            controller.broadcastStopLyric()
            log("prepareSongs:Before", dumpState(position))
            return synchronized(this) {
                queueManager.modifyPosition(position, false)
                // playerState = PlayerState.PREPARING
                prepareCurrentPlayer(queueManager.currentSong).also { success ->
                    if (success) prepareNextTrack() else cancelNextTrackPreparation()

                    controller.updateLyrics()

                    log("prepareSongs:After", dumpState(position))
                }
            }
        }

        override fun playAt(position: Int) {
            if (prepareSongs(position)) {
                play()
            } else {
                val currentSong = queueManager.currentSong
                if (currentSong != null) {
                    controller.handler.post { checkFile(currentSong.data) }
                    if (queueManager.repeatMode != RepeatMode.REPEAT_SINGLE_SONG) {
                        jumpForward(false)
                    }
                }
            }
            log("playAtImp", dumpState(position))
        }

        override fun play() {
            if (queueManager.playingQueue.isNotEmpty()) {
                if (audioFocusManager.requestAudioFocus()) {
                    checkAndRegisterBecomingNoisyReceiver(service)
                    if (!audioPlayer.isPlaying) {
                        // Actual Logics Start
                        synchronized(this) {
                            if (!audioPlayer.isInitialized) {
                                playAt(queueManager.currentSongPosition)
                            } else {
                                audioPlayer.play()

                                controller.playerState = PlayerState.PLAYING
                                pauseReason = PauseReason.NOT_PAUSED
                                controller.acquireWakeLock(
                                    abs(songDurationMillis - songProgressMillis) + 1500L
                                )
                                handler.removeMessages(ControllerHandler.DUCK)
                                handler.sendEmptyMessage(ControllerHandler.UNDUCK)
                                handler.loop() // start broadcast lyrics loop
                            }
                        }
                        // Actual Logics End
                    } else {
                        log("playImp", "Already Playing!", true)
                    }
                } else {
                    Toast.makeText(
                        service,
                        service.resources.getString(R.string.err_audio_focus_denied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                pause(true, reason = PauseReason.PAUSE_FOR_QUEUE_ENDED)
            }
        }

        override fun pause(releaseResource: Boolean, reason: Int) {
            if (audioPlayer.pause()) {
                pauseReason = reason
                controller.broadcastStopLyric()
                controller.playerState = PlayerState.PAUSED
                if (releaseResource) controller.releaseTakenResources()
            } else {
                log("pause", "Failed!")
            }
        }

        override fun stop() {
            audioPlayer.stop(release = false)
            controller.broadcastStopLyric()
            controller.playerState = PlayerState.STOPPED
            controller.releaseTakenResources()
        }

        override fun togglePlayPause() {
            if (audioPlayer.isPlaying) {
                pause(false, reason = PauseReason.PAUSE_BY_MANUAL_ACTION)
            } else {
                play()
            }
        }

        @PauseReason
        override var pauseReason: Int = PauseReason.NOT_PAUSED

        override val isPlaying: Boolean
            get() = audioPlayer.isInitialized && audioPlayer.isPlaying

        override val songProgressMillis: Int
            get() = if (audioPlayer.isInitialized) audioPlayer.position() else -1

        override val songDurationMillis: Int
            get() = if (audioPlayer.isInitialized) audioPlayer.duration() else -1


        override fun seekTo(position: Long) {
            audioPlayer.seek(position.toInt())
        }

        override fun rewindToBeginning() {
            seekTo(0)
            service.requireRefreshMediaSessionState()
        }

        override fun jumpBackward(force: Boolean) {
            if (queueManager.playingQueue.isEmpty()) return
            val position =
                if (force) {
                    queueManager.previousLoopPosition
                } else {
                    queueManager.previousSongPosition
                }
            playAt(position)
        }

        override fun back(force: Boolean) {
            if (audioPlayer.position() > 5000) {
                rewindToBeginning()
            } else {
                jumpBackward(force)
            }
        }

        override fun jumpForward(force: Boolean) {
            if (queueManager.playingQueue.isEmpty()) return
            val position =
                if (force) {
                    queueManager.nextLoopPosition
                } else {
                    queueManager.nextSongPosition
                }
            if (position >= 0) {
                playAt(position)
            } else {
                pause(false, reason = PauseReason.PAUSE_FOR_QUEUE_ENDED)
            }
        }

        override val playerSpeed: Float get() = audioPlayer.speed

        override fun setPlayerSpeed(speed: Float) {
            handler.request {
                audioPlayer.speed = speed
            }
        }

        override fun setVolume(vol: Float) {
            audioPlayer.setVolume(vol)
        }

        override val audioSessionId: Int get() = audioPlayer.audioSessionId

        override var gaplessPlayback
            get() = audioPlayer.gaplessPlayback
            set(value) {
                audioPlayer.gaplessPlayback = value
            }

        override fun onTrackWentToNext() {
            handler.request {
                if (controller.checkSleepTimer()) return@request
                queueManager.moveToNextSong(false)
                controller.updateLyrics()
            }
        }

        override fun onTrackEnded() {
            handler.request {
                if (controller.checkSleepTimer()) return@request
                if (queueManager.isQueueEnded()) {
                    pause(false, reason = PauseReason.PAUSE_FOR_QUEUE_ENDED)
                } else {
                    playAt(queueManager.nextSongPosition)
                    controller.updateLyrics()
                }
            }
        }

        override fun onError(what: Int, extra: Int) {
            val msg = makeErrorMessage(service.resources, what, extra, audioPlayer.currentDataSource)
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(service, msg, Toast.LENGTH_SHORT).show()
            }
            if (queueManager.isQueueEnded()) {
                handler.request {
                    pause(false, reason = PauseReason.PAUSE_FOR_QUEUE_ENDED)
                }
            } else if (controller.shouldAutoSkipUnplayable) {
                handler.request {
                    playAt(queueManager.nextSongPosition)
                    controller.updateLyrics()
                }
            } else {
                pause(false, reason = PauseReason.PAUSE_ERROR)
            }
        }

        private fun dumpState(position: Int): String =
            "<@$position> current:${queueManager.currentSong?.title}, next:${queueManager.nextSong?.title}, state: ${controller.playerState}"

        private fun checkFile(path: String) {
            val exists = try {
                File(path).exists()
            } catch (_: SecurityException) {
                false
            }
            Toast.makeText(
                service,
                makeErrorMessage(service.resources, path, exists),
                Toast.LENGTH_SHORT
            ).show()
        }

        private fun log(where: String, msg: String, force: Boolean = false) {
            if (DEBUG || force) Log.i("ControllerImpl", "<$where>$msg")
        }

        //region BecomingNoisyReceiver
        private var becomingNoisyReceiverRegistered = false
        private fun checkAndRegisterBecomingNoisyReceiver(context: Context) {
            if (!becomingNoisyReceiverRegistered) {
                ContextCompat.registerReceiver(
                    context,
                    becomingNoisyReceiver,
                    IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                    ContextCompat.RECEIVER_EXPORTED
                )
                becomingNoisyReceiverRegistered = true
            }
        }

        private fun unregisterBecomingNoisyReceiver(context: Context) {
            if (becomingNoisyReceiverRegistered) {
                context.unregisterReceiver(becomingNoisyReceiver)
                becomingNoisyReceiverRegistered = false
            }
        }

        private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    handler.request {
                        pause(releaseResource = true, reason = PauseReason.PAUSE_FOR_AUDIO_BECOMING_NOISY)
                    }
                }
            }
        }
        //endregion

    }

    class VanillaAudioPlayerControllerImpl(controller: PlayerController) : AudioPlayerControllerImpl(controller) {
        override fun initPlayer(musicService: MusicService): Playback =
            VanillaAudioPlayer(musicService, false, this)
    }

    class ExoAudioPlayerControllerImpl(controller: PlayerController) : AudioPlayerControllerImpl(controller) {
        override fun initPlayer(musicService: MusicService): Playback =
            ExoAudioPlayer(musicService, controller.handler, false, this)
    }

    override var gaplessPlayback: Boolean
        get() = impl.gaplessPlayback
        set(value) {
            impl.gaplessPlayback = value
        }

    override fun prepareNextTrack() {
        handler.post {
            if (impl.gaplessPlayback) impl.prepareNextTrack() else impl.cancelNextTrackPreparation()
        }
    }

    override fun cancelNextTrackPreparation() {
        handler.post {
            impl.cancelNextTrackPreparation()
        }
    }

    /**
     * Play songs from a certain position
     */
    override fun playAt(position: Int) = handler.request {
        impl.playAt(position)
    }

    /**
     * continue play
     */
    override fun play() = handler.request {
        impl.play()
    }

    override val pauseReason: Int get() = impl.pauseReason

    /**
     * Pause
     * @param releaseResource false if not release taken resource
     * @param reason cause of this pause (see [PauseReason])
     */
    override fun pause(releaseResource: Boolean, @PauseReason reason: Int) = handler.request {
        impl.pause(releaseResource, reason)
    }

    override fun togglePlayPause() = handler.request {
        impl.togglePlayPause()
    }

    override val isPlaying: Boolean get() = impl.isPlaying

    /**
     * Jump to beginning of this song
     */
    override fun rewindToBeginning() = handler.request {
        impl.rewindToBeginning()
    }

    /**
     * Return to previous song
     */
    override fun jumpBackward(force: Boolean) = handler.request {
        impl.jumpBackward(force)
    }

    /**
     * [rewindToBeginning] or [jumpBackward]
     */
    override fun back(force: Boolean) = handler.request {
        impl.back(force)
    }

    /**
     * Skip and jump to next song
     */
    override fun jumpForward(force: Boolean) = handler.request {
        impl.jumpForward(force)
    }

    /**
     * Move current time to [position]
     * @param position time in millisecond
     */
    override fun seekTo(position: Long) = handler.request {
        impl.seekTo(position)
    }

    override fun stop() = handler.request {
        impl.stop()
        if (quitRequestPending.get()) {
            clearSleepTimer()
            handler.postDelayed({ service.doQuit() }, 200)
        }
    }

    private var sleepTimerAlarm: AlarmManager.OnAlarmListener? = null
    private val sleepTimerDeadline = AtomicLong(TIMER_OFF)

    fun hasSleepTimer(): Boolean = sleepTimerDeadline.get() != TIMER_OFF
    fun sleepTimerElapsedTime(): Long = sleepTimerDeadline.get().takeIf { it > 0 } ?: TIMER_OFF

    fun setSleepTimer(minutesToQuit: Long, shouldFinishLastSong: Boolean): Boolean {
        val timerHandler = _handler ?: return false
        return timerHandler.post {
            clearSleepTimer()
            val deadline = SystemClock.elapsedRealtime() + minutesToQuit * 60_000L
            val listener = object : AlarmManager.OnAlarmListener {
                override fun onAlarm() {
                    if (sleepTimerAlarm !== this) return
                    sleepTimerAlarm = null
                    if (shouldFinishLastSong) {
                        sleepTimerDeadline.set(FINISH_CURRENT_TRACK)
                    } else {
                        quitFromSleepTimer()
                    }
                }
            }
            try {
                val alarmManager = alarmManager()
                if (alarmManager != null) {
                    alarmManager.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        deadline,
                        "SleepTimer",
                        listener,
                        timerHandler
                    )
                    sleepTimerAlarm = listener
                    sleepTimerDeadline.set(deadline)
                }
            } catch (e: Exception) {
                Log.w("PlayerController", "Failed to set sleep timer", e)
            }
        }
    }

    fun cancelSleepTimer(): Boolean = _handler?.post { clearSleepTimer() } ?: false

    private fun clearSleepTimer() {
        val listener = sleepTimerAlarm
        sleepTimerAlarm = null
        sleepTimerDeadline.set(TIMER_OFF)
        if (listener != null) alarmManager()?.cancel(listener)
    }

    private fun checkSleepTimer(): Boolean {
        if (quitRequestPending.get() || _service?.isDestroyed != false) return true
        val deadline = sleepTimerDeadline.get()
        if (deadline == TIMER_OFF || deadline > SystemClock.elapsedRealtime()) return false
        quitFromSleepTimer()
        return true
    }

    private fun quitFromSleepTimer() {
        clearSleepTimer()
        val musicService = _service ?: return
        quitRequestPending.set(true)
        Handler(Looper.getMainLooper()).post {
            if (!musicService.isDestroyed) musicService.requestToQuit()
        }
    }

    /**
     * true to mark the state for quitting service
     */
    val quitRequestPending = AtomicBoolean(false)

    var resumeAfterAudioFocusGain: Boolean = false

    var ignoreAudioFocus: Boolean = false

    private var audioDucking: Boolean = true

    private var broadcastSynchronizedLyrics: Boolean = false

    private var shouldAutoSkipUnplayable: Boolean = false

    fun saveCurrentMills() = handler.request {
        QueuePreferenceManager(service).currentMillisecond = impl.songProgressMillis
    }

    override val audioSessionId: Int get() = impl.audioSessionId

    override val songProgressMillis: Int get() = impl.songProgressMillis

    override val songDurationMillis: Int get() = impl.songDurationMillis

    override val playerSpeed
        get() = impl.playerSpeed

    override fun setPlayerSpeed(speed: Float) {
        handler.request { impl.setPlayerSpeed(speed) }
    }

    override fun setVolume(vol: Float) = handler.request {
        impl.setVolume(vol)
    }

    var enableLyrics: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                updateLyrics()
            } else {
                lyricsBroadcastController.clear()
                broadcastStopLyric()
            }
        }

    private fun broadcastStopLyric() = lyricsBroadcastController.stop()
    fun replaceLyrics(lyrics: LrcLyrics?) {
        if (!enableLyrics) return
        if (lyrics != null) {
            lyricsBroadcastController.load(lyrics)
        } else {
            lyricsBroadcastController.clear()
        }
    }

    fun updateLyrics() {
        if (!enableLyrics) return
        service.coroutineScope.launch {
            val currentSong = queueManager.currentSong
            if (currentSong != null) {
                lyricsBroadcastController.load(service, currentSong)
            } else {
                lyricsBroadcastController.clear()
            }
        }
    }

    private fun alarmManager(): AlarmManager? =
        _service?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private val observers: MutableList<PlayerStateObserver> = ArrayList()
    fun addObserver(observer: PlayerStateObserver) = observers.add(observer)
    fun removeObserver(observer: PlayerStateObserver): Boolean = observers.remove(observer)

    class ControllerHandler(private val playerController: PlayerController, looper: Looper) : Handler(looper) {
        /**
         * Request running in the handler thread
         * @param request RunnableRequest: (PlayerController) -> Unit
         */
        fun request(request: (PlayerController) -> Unit) {
            post { request(playerController) }
        }

        fun command(what: Int, arg1: Int, arg2: Int) {
            sendMessage(
                Message.obtain(this, what, arg1, arg2)
            )
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {

                DUCK   -> {
                    if (playerController.audioDucking) {
                        currentDuckVolume -= .05f
                        if (currentDuckVolume > .2f) {
                            sendEmptyMessageDelayed(DUCK, 10)
                        } else {
                            currentDuckVolume = .2f
                        }
                    } else {
                        currentDuckVolume = 1f
                    }
                    playerController.impl.setVolume(currentDuckVolume)

                }

                UNDUCK -> {
                    if (playerController.audioDucking) {
                        currentDuckVolume += .03f
                        if (currentDuckVolume < 1f) {
                            sendEmptyMessageDelayed(UNDUCK, 10)
                        } else {
                            currentDuckVolume = 1f
                        }
                    } else {
                        currentDuckVolume = 1f
                    }
                    playerController.impl.setVolume(currentDuckVolume)

                }

            }
        }

        private var currentDuckVolume = 1.0f

        /**
         * @return true if continue
         */
        private fun broadcastLyrics(): Boolean {
            if (playerController.playerState != PlayerState.PLAYING || playerController.broadcastSynchronizedLyrics) {
                post {
                    playerController.broadcastStopLyric()
                }
                return false
            } else {
                post {
                    playerController.lyricsBroadcastController.broadcast(playerController.songProgressMillis)
                }
                return true
            }
        }

        fun loop() {
            postDelayed({
                if (broadcastLyrics()) loop()
            }, LOOP_INTERVAL)
        }

        companion object {

            const val DUCK = 20
            const val UNDUCK = 30

            private const val LOOP_INTERVAL = 750L
        }
    }

    companion object {
        private const val TIMER_OFF = -1L
        private const val FINISH_CURRENT_TRACK = 0L
    }
}