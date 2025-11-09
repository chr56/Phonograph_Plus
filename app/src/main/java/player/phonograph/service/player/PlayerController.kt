package player.phonograph.service.player

import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.foundation.mediastore.mediaStoreUriSongExternal
import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.service.PlayerState
import player.phonograph.model.service.PlayerStateObserver
import player.phonograph.model.service.RepeatMode
import player.phonograph.service.MusicService
import player.phonograph.service.ServiceComponent
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.util.LyricsUpdater
import player.phonograph.service.util.QueuePreferenceManager
import player.phonograph.service.util.makeErrorMessage
import player.phonograph.settings.Keys
import player.phonograph.settings.SettingObserver
import androidx.core.content.ContextCompat
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
import android.util.Log
import android.widget.Toast
import kotlin.math.abs
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

// todo cleanup queueManager.setQueueCursor
/**
 * @author chr_56 & Abou Zeid (kabouzeid) (original author)
 */
class PlayerController : ServiceComponent, Controller {

    private var _service: MusicService? = null
    val service: MusicService get() = _service!!


    private var _impl: ControllerInternal? = null
    private val impl: ControllerInternal get() = _impl!!

    private var _wakeLock: WakeLock? = null
    private val wakeLock: WakeLock get() = _wakeLock!!


    private var _handler: ControllerHandler? = null
    val handler: ControllerHandler get() = _handler!!

    private var _thread: HandlerThread? = null
    private val thread: HandlerThread get() = _thread!!

    private val queueManager: QueueManager get() = service.queueManager

    private var _lyricsUpdater: LyricsUpdater? = null
    private val lyricsUpdater: LyricsUpdater get() = _lyricsUpdater!!

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


        _lyricsUpdater = LyricsUpdater()
        lyricsUpdater.onCreate(service)

        created = true

        observeSettings(musicService)
    }

    override fun onDestroy(musicService: MusicService) {
        created = false

        lyricsUpdater.onDestroy(musicService)
        _lyricsUpdater = null

        impl.stop()
        impl.onDestroy(musicService)
        _impl = null

        audioFocusManager.abandonAudioFocus()
        releaseWakeLock()

        _wakeLock = null
        _audioFocusManager = null


        thread.quitSafely()
        handler.looper.quitSafely()
        _thread = null
        _handler = null

        _service = null
    }


    private fun observeSettings(service: MusicService) {
        val settingObserver = SettingObserver(service, service.coroutineScope)
        settingObserver.collect(Keys.audioDucking) { value ->
            audioDucking = value
        }
        settingObserver.collect(Keys.resumeAfterAudioFocusGain) { value ->
            resumeAfterAudioFocusGain = value
        }
        settingObserver.collect(Keys.alwaysPlay) { value ->
            ignoreAudioFocus = value
        }
        settingObserver.collect(Keys.gaplessPlayback) { gaplessPlayback ->
            handler.post {
                val controllerImpl = _impl
                if (controllerImpl is VanillaAudioPlayerControllerImpl && controllerImpl.created) {
                    controllerImpl.gaplessPlayback = gaplessPlayback
                    controllerImpl.prepareNextPlayer(
                        if (gaplessPlayback) queueManager.nextSong else null
                    )
                }
            }
        }
        settingObserver.collect(Keys.broadcastSynchronizedLyrics) { value ->
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

    class VanillaAudioPlayerControllerImpl(
        val controller: PlayerController,
    ) : ControllerInternal, Playback.PlaybackCallbacks {

        override var created: Boolean = false

        private var _service: MusicService? = null
        val service: MusicService get() = _service!!

        private var _audioPlayer: Playback? = null
        private val audioPlayer: Playback get() = _audioPlayer!!

        private var _queueManager: QueueManager? = null
        private val queueManager: QueueManager get() = _queueManager!!

        private val handler: ControllerHandler get() = controller.handler
        private val audioFocusManager: AudioFocusManager get() = controller.audioFocusManager

        override fun onCreate(musicService: MusicService) {
            _service = musicService

            _audioPlayer = VanillaAudioPlayer(musicService, false, this)

            _queueManager = musicService.queueManager

            created = true
            restore(musicService)
        }

        override fun onDestroy(musicService: MusicService) {
            unregisterBecomingNoisyReceiver(musicService)
            audioPlayer.release()

            created = false

            _queueManager = null
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


        /**
         * prepare current player data source safely
         * @param song what to play now
         * @return true if success
         */
        private fun prepareCurrentPlayer(song: Song?): Boolean {
            return if (song != null && song.data.isNotEmpty()) {
                audioPlayer.setDataSource(mediaStoreUriSongExternal(song.id).toString())
            } else {
                false
            }
        }

        /**
         * prepare next player data source safely
         * @param song what to play now
         */
        fun prepareNextPlayer(song: Song?) {
            if (audioPlayer.isInitialized) audioPlayer.setNextDataSource(
                if (song != null && song.data.isNotEmpty())
                    mediaStoreUriSongExternal(song.id).toString()
                else null
            )
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
                prepareCurrentPlayer(queueManager.currentSong).also { setCurrentSuccess ->
                    prepareNextPlayer(if (setCurrentSuccess) queueManager.nextSong else null)

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
                                handler.lyricsLoop() // start broadcast lyrics loop
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
            audioPlayer.stop()
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

        override var playerSpeed
            get() = audioPlayer.speed
            set(speed) = handler.request {
                audioPlayer.speed = speed
            }

        override fun setVolume(vol: Float) {
            audioPlayer.setVolume(vol)
        }

        override val audioSessionId: Int get() = audioPlayer.audioSessionId

        var gaplessPlayback
            get() = audioPlayer.gaplessPlayback
            set(value) {
                audioPlayer.gaplessPlayback = value
            }

        override fun onTrackWentToNext() {
            // check sleep timer
            if (controller.quitAfterFinishCurrentSong) {
                handler.request { stop() }
                return
            }
            handler.request {
                queueManager.moveToNextSong(false)
                controller.updateLyrics()
            }
        }

        override fun onTrackEnded() {
            // check sleep timer
            if (controller.quitAfterFinishCurrentSong) {
                handler.request { stop() }
                return
            }
            if (queueManager.isQueueEnded()) {
                handler.request {
                    pause(false, reason = PauseReason.PAUSE_FOR_QUEUE_ENDED)
                }
            } else {
                handler.request {
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
            pauseReason = PauseReason.PAUSE_ERROR
        }

        private fun dumpState(position: Int): String =
            "<@$position> current:${queueManager.currentSong?.title}, next:${queueManager.nextSong?.title}, state: ${controller.playerState}"

        private fun checkFile(path: String) {
            val exists = try {
                File(path).exists()
            } catch (e: SecurityException) {
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

    fun prepareNext() {
        handler.post {
            val controllerImpl = impl
            if (controllerImpl is VanillaAudioPlayerControllerImpl) {
                controllerImpl.prepareNextPlayer(
                    if (controllerImpl.gaplessPlayback) queueManager.nextSong else null
                )
            }
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
    }

    /**
     * true if you want to stop player when current track is ended
     * Used by Sleep Timer
     */
    var quitAfterFinishCurrentSong: Boolean = false
        set(value) {
            synchronized(this) {
                field = value
            }
        }

    var resumeAfterAudioFocusGain: Boolean = false

    var ignoreAudioFocus: Boolean = false

    private var audioDucking: Boolean = true

    private var broadcastSynchronizedLyrics: Boolean = false

    fun saveCurrentMills() = handler.request {
        QueuePreferenceManager(service).currentMillisecond = impl.songProgressMillis
    }

    override val audioSessionId: Int get() = impl.audioSessionId

    override fun setVolume(vol: Float) = handler.request {
        impl.setVolume(vol)
    }

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
                    playerController.lyricsUpdater.broadcast(playerController.songProgressMillis)
                }
                return true
            }
        }

        /**
         * start loop to broadcast lyrics
         */
        fun lyricsLoop() {
            postDelayed({
                if (broadcastLyrics()) lyricsLoop()
            }, 750)
        }

        companion object {

            const val DUCK = 20
            const val UNDUCK = 30

        }
    }

    override val songProgressMillis: Int get() = impl.songProgressMillis

    override val songDurationMillis: Int get() = impl.songDurationMillis

    override var playerSpeed
        get() = impl.playerSpeed
        set(speed) = handler.request {
            impl.playerSpeed = speed
        }

    private fun broadcastStopLyric() = StatusBarLyric.stopLyric()
    fun replaceLyrics(lyrics: LrcLyrics?) {
        if (lyrics != null) {
            lyricsUpdater.updateViaLyrics(lyrics)
        } else {
            lyricsUpdater.clear()
        }
    }

    fun updateLyrics() {
        service.coroutineScope.launch(SupervisorJob()) {
            lyricsUpdater.updateViaSong(service, queueManager.currentSong)
        }
    }

}