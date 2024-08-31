package player.phonograph.service.player

import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.service.MusicService
import player.phonograph.service.ServiceComponent
import player.phonograph.service.queue.QueueManager
import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.util.LyricsUpdater
import player.phonograph.service.util.QueuePreferenceManager
import player.phonograph.service.util.makeErrorMessage
import player.phonograph.settings.Keys
import player.phonograph.settings.PrimitiveKey
import player.phonograph.settings.Setting
import player.phonograph.util.registerReceiverCompat
import androidx.core.content.ContextCompat
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
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

        _impl = ControllerImpl()
        impl.onCreate(musicService)


        _lyricsUpdater = LyricsUpdater()
        lyricsUpdater.onCreate(service)

        observeSettings(musicService)
    }

    override fun onDestroy(musicService: MusicService) {

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
        fun <T> collect(key: PrimitiveKey<T>, collector: FlowCollector<T>) {
            service.coroutineScope.launch(SupervisorJob()) {
                Setting(service)[key].flow.distinctUntilChanged().collect(collector)
            }
        }
        collect(Keys.audioDucking) { value ->
            audioDucking = value
        }
        collect(Keys.resumeAfterAudioFocusGain) { value ->
            resumeAfterAudioFocusGain = value
        }
        collect(Keys.alwaysPlay) { value ->
            ignoreAudioFocus = value
        }
        collect(Keys.gaplessPlayback) { gaplessPlayback ->
            val controllerImpl = impl
            if (controllerImpl is ControllerImpl) {
                handler.post {
                    controllerImpl.gaplessPlayback = gaplessPlayback
                    controllerImpl.prepareNextPlayer(
                        if (gaplessPlayback) queueManager.nextSong else null
                    )
                }
            }
        }
        collect(Keys.broadcastSynchronizedLyrics) { value ->
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
                observers.executeForEach { onPlayerStateChanged(oldState, value) }
            }
        }


    interface ControllerInternal : Controller, ServiceComponent {

        fun playAt(position: Int)

        /**
         * prepare current player data source safely
         * @param song what to play now
         * @return true if success
         */
        fun prepareCurrentPlayer(song: Song): Boolean

        /**
         * prepare next player data source safely
         * @param song what to play now
         */
        fun prepareNextPlayer(song: Song?)

        fun saveCurrentMills()
    }


    inner class ControllerImpl : ControllerInternal, Playback.PlaybackCallbacks {

        private var _audioPlayer: Playback? = null
        private val audioPlayer: Playback get() = _audioPlayer!!


        override fun onCreate(musicService: MusicService) {
            _audioPlayer = VanillaAudioPlayer(musicService, false, this)

            restore(musicService)
        }

        override fun onDestroy(musicService: MusicService) {
            unregisterBecomingNoisyReceiver(musicService)
            audioPlayer.release()
            _audioPlayer = null
        }

        private fun restore(musicService: MusicService) {
            if (prepareCurrentPlayer(queueManager.currentSong)) {
                val restored = QueuePreferenceManager(musicService).currentMillisecond
                if (restored > 0) seekTo(restored.toLong())
            }
        }


        override fun prepareCurrentPlayer(song: Song): Boolean {
            return if (song != Song.EMPTY_SONG) {
                audioPlayer.setDataSource(getTrackUri(song.id).toString())
            } else {
                false
            }
        }


        override fun prepareNextPlayer(song: Song?) {
            audioPlayer.setNextDataSource(
                if (song != null && song != Song.EMPTY_SONG) getTrackUri(song.id).toString() else null
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
            broadcastStopLyric()
            log("prepareSongs:Before", dumpState(position))
            return synchronized(this) {
                queueManager.modifyPosition(position, false)
                // playerState = PlayerState.PREPARING
                prepareCurrentPlayer(queueManager.currentSong).also { setCurrentSuccess ->
                    prepareNextPlayer(if (setCurrentSuccess) queueManager.nextSong else null)

                    notifyNowPlayingChanged()

                    log("prepareSongs:After", dumpState(position))
                }
            }
        }

        override fun playAt(position: Int) {
            if (prepareSongs(position)) {
                play()
            } else {
                handler.post { checkFile(queueManager.currentSong.data) }
                if (queueManager.repeatMode != RepeatMode.REPEAT_SINGLE_SONG) {
                    jumpForward(false)
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

                                playerState = PlayerState.PLAYING
                                pauseReason = PauseReason.NOT_PAUSED
                                acquireWakeLock(
                                    queueManager.currentSong.duration - audioPlayer.position() + 1000L
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
                        service.resources.getString(R.string.audio_focus_denied),
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
                broadcastStopLyric()
                playerState = PlayerState.PAUSED
                if (releaseResource) releaseTakenResources()
            } else {
                log("pause", "Failed!")
            }
        }

        override fun stop() {
            audioPlayer.stop()
            broadcastStopLyric()
            playerState = PlayerState.STOPPED
            releaseTakenResources()
            observers.executeForEach {
                onReceivingMessage(MSG_PLAYER_STOPPED)
            }
        }

        override fun togglePlayPause() {
            if (audioPlayer.isPlaying) {
                pause(false, reason = PauseReason.PAUSE_BY_MANUAL_ACTION)
            } else {
                play()
            }
        }

        override val isPlaying: Boolean
            get() = audioPlayer.isInitialized && audioPlayer.isPlaying

        override val songProgressMillis: Int
            get() = if (audioPlayer.isInitialized) audioPlayer.position() else -1

        override val songDurationMillis: Int
            get() = if (audioPlayer.isInitialized) audioPlayer.duration() else -1


        override fun seekTo(position: Long): Int {
            return audioPlayer.seek(position.toInt())
        }

        override fun rewindToBeginning() {
            seekTo(0)
            service.requireRefreshMediaSessionState()
        }

        override fun jumpBackward(force: Boolean) {
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
                observers.executeForEach {
                    onReceivingMessage(MSG_NO_MORE_SONGS)
                }
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
            if (quitAfterFinishCurrentSong) {
                handler.request { stop() }
                return
            }
            handler.request {
                queueManager.moveToNextSong(false)
                notifyNowPlayingChanged()
                prepareNextPlayer(queueManager.nextSong)
            }
        }

        override fun onTrackEnded() {
            // check sleep timer
            if (quitAfterFinishCurrentSong) {
                handler.request { stop() }
                return
            }
            if (queueManager.isQueueEnded()) {
                handler.request {
                    pause(true, reason = PauseReason.PAUSE_FOR_QUEUE_ENDED)
                }
                broadcastStopLyric()
                observers.executeForEach {
                    onReceivingMessage(MSG_NO_MORE_SONGS)
                }
            } else {
                handler.request {
                    prepareNextPlayer(queueManager.nextSong)
                    queueManager.moveToNextSong(false)
                    playAt(queueManager.currentSongPosition)
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
            "<@$position> current:${queueManager.currentSong.title}, next:${queueManager.nextSong.title}, state: $playerState"

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

        override fun saveCurrentMills() {
            QueuePreferenceManager(service).currentMillisecond = audioPlayer.position()
        }

        //region BecomingNoisyReceiver
        private var becomingNoisyReceiverRegistered = false
        private fun checkAndRegisterBecomingNoisyReceiver(context: Context) {
            if (!becomingNoisyReceiverRegistered) {
                context.registerReceiverCompat(
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

    private fun notifyNowPlayingChanged() {
        observers.executeForEach {
            onReceivingMessage(MSG_NOW_PLAYING_CHANGED)
        }
        service.coroutineScope.launch(SupervisorJob()) {
            lyricsUpdater.updateViaSong(queueManager.currentSong)
        }
    }

    fun prepareNext() {
        handler.post {
            val controllerImpl = impl
            if (controllerImpl is ControllerImpl) {
                controllerImpl.prepareNextPlayer(
                    if (controllerImpl.gaplessPlayback) queueManager.nextSong else null
                )
            }
        }
    }

    /**
     * Play songs from a certain position
     */
    fun playAt(position: Int) = handler.request {
        impl.playAt(position)
    }

    /**
     * continue play
     */
    override fun play() = handler.request {
        impl.play()
    }

    @PauseReason
    var pauseReason: Int = PauseReason.NOT_PAUSED

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
    override fun seekTo(position: Long): Int = synchronized(impl) {
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

    companion object {
        private fun getTrackUri(songId: Long): Uri =
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
    }

    fun saveCurrentMills() = handler.request {
        impl.saveCurrentMills()
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

                DUCK                   -> {
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

                UNDUCK                 -> {
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

                RE_PREPARE_NEXT_PLAYER -> synchronized(playerController.impl) {
                    playerController.impl.prepareNextPlayer(playerController.queueManager.nextSong)
                }


                CLEAN_NEXT_PLAYER      -> synchronized(playerController.impl) {
                    playerController.impl.prepareNextPlayer(null)
                }

            }
        }

        private var currentDuckVolume = 1.0f

        /**
         * @return true if continue
         */
        private fun broadcastLyrics(): Boolean {
            if (playerController.playerState != PlayerState.PLAYING || playerController.broadcastSynchronizedLyrics) {
                playerController.broadcastStopLyric()
                return false
            }
            playerController.lyricsUpdater.broadcast(playerController.songProgressMillis)
            return true
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

            const val RE_PREPARE_NEXT_PLAYER = 40
            const val CLEAN_NEXT_PLAYER = 41
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

    private fun log(where: String, msg: String, force: Boolean = false) {
        if (DEBUG || force) Log.i("PlayerController", "※$msg @$where")
    }

}