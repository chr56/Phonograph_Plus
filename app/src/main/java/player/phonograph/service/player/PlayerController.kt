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
class PlayerController : ServiceComponent, Playback.PlaybackCallbacks, Controller {

    private var _service: MusicService? = null
    val service: MusicService get() = _service!!


    private var _audioPlayer: Playback? = null
    private val audioPlayer: Playback get() = _audioPlayer!!

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

        _audioPlayer = VanillaAudioPlayer(service, false /* default */, this)

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


        _lyricsUpdater = LyricsUpdater()
        lyricsUpdater.onCreate(service)


        restorePosition()

        observeSettings(musicService)
    }

    override fun onDestroy(musicService: MusicService) {

        stopImp()
        unregisterBecomingNoisyReceiver(service)

        lyricsUpdater.onDestroy(musicService)
        _lyricsUpdater = null

        thread.quitSafely()
        handler.looper.quitSafely()
        _thread = null
        _handler = null


        releaseWakeLock()
        audioFocusManager.abandonAudioFocus()
        audioPlayer.release()
        _wakeLock = null
        _audioFocusManager = null
        _audioPlayer = null

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
            audioPlayer.gaplessPlayback = gaplessPlayback
            handler.apply {
                if (gaplessPlayback) {
                    removeMessages(ControllerHandler.RE_PREPARE_NEXT_PLAYER)
                    sendEmptyMessage(ControllerHandler.RE_PREPARE_NEXT_PLAYER)
                } else {
                    removeMessages(ControllerHandler.CLEAN_NEXT_PLAYER)
                    sendEmptyMessage(ControllerHandler.CLEAN_NEXT_PLAYER)
                }
            }
        }
        collect(Keys.broadcastSynchronizedLyrics) { value ->
            broadcastSynchronizedLyrics = value
        }
    }

    private fun restorePosition() {
        prepareCurrentPlayer(queueManager.currentSong).let { success ->
            if (success) {
                val restoredPositionInTrack = QueuePreferenceManager(service).currentMillisecond
                if (restoredPositionInTrack > 0) seekTo(restoredPositionInTrack.toLong())
            }
        }
    }

    private var broadcastSynchronizedLyrics: Boolean = false

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

    /**
     * prepare current and next player and set queue cursor(position)
     * @param position where to start in queue
     * @return true if it is ready
     */
    private fun prepareSongsImp(position: Int): Boolean {
        if (position < 0) {
            log("prepareSongsImp", "illegal position")
            return false
        }
        // todo: change STATE if possible
        broadcastStopLyric()
        log(
            "prepareSongsImp:Before",
            "current:${queueManager.currentSong.title} ,next:${queueManager.nextSong.title}"
        )
        return synchronized(this) {
            queueManager.modifyPosition(position, false)
            prepareCurrentPlayer(queueManager.currentSong).also { setCurrentSuccess ->
                prepareNextPlayer(if (setCurrentSuccess) queueManager.nextSong else null)

                notifyNowPlayingChanged()

                log(
                    "prepareSongsImp:After",
                    "current:${queueManager.currentSong.title}, next:${queueManager.nextSong.title}"
                )
            }
        }
    }

    private fun notifyNowPlayingChanged() {
        observers.executeForEach {
            onReceivingMessage(MSG_NOW_PLAYING_CHANGED)
        }
        service.coroutineScope.launch(SupervisorJob()) {
            lyricsUpdater.updateViaSong(queueManager.currentSong)
        }
    }

    /**
     * prepare current player data source safely
     * @param song what to play now
     * @return true if success
     */
    private fun prepareCurrentPlayer(song: Song): Boolean {
        return if (song != Song.EMPTY_SONG) audioPlayer.setDataSource(
            getTrackUri(song.id).toString()
        ) else {
            false
        }
    }

    /**
     * prepare next player data source safely
     * @param song what to play now
     */
    private fun prepareNextPlayer(song: Song?) {
        audioPlayer.setNextDataSource(
            if (song != null && song != Song.EMPTY_SONG) getTrackUri(song.id).toString() else null
        )
    }

    /**
     * Play songs from a certain position
     */
    fun playAt(position: Int) = handler.request {
        it.playAtImp(position)
    }

    private fun playAtImp(position: Int) {
        if (prepareSongsImp(position)) {
            playImp()
        } else {
            val path = queueManager.currentSong.data
            handler.post {
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
            if (queueManager.repeatMode != RepeatMode.REPEAT_SINGLE_SONG) {
                jumpForwardImp(false)
            }
        }
        log("playAtImp", "current: at $position song(${queueManager.currentSong.title})")
    }

    /**
     * Set position in current queue, only available if paused
     * @param position current position in playing queue
     */
    fun setPosition(position: Int) = handler.request {
        it.setPositionImp(position)
    }

    private fun setPositionImp(position: Int) {
        if (playerState == PlayerState.PAUSED) {
            queueManager.modifyPosition(position, false)
            prepareSongsImp(position)
        }
    }

    /**
     * continue play
     */
    override fun play() = handler.request {
        it.playImp()
    }

    private fun playImp() {
        if (queueManager.playingQueue.isNotEmpty()) {
            if (audioFocusManager.requestAudioFocus()) {
                checkAndRegisterBecomingNoisyReceiver(service)
                if (!audioPlayer.isPlaying) {
                    // Actual Logics Start
                    synchronized(this) {
                        if (!audioPlayer.isInitialized) {
                            playAtImp(queueManager.currentSongPosition)
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
            pauseImp(force = true, reason = PauseReason.PAUSE_FOR_QUEUE_ENDED)
        }
    }

    @PauseReason
    var pauseReason: Int = PauseReason.NOT_PAUSED

    /**
     * Pause
     * @param releaseResource false if not release taken resource
     * @param reason cause of this pause (see [PauseReason])
     */
    override fun pause(releaseResource: Boolean, @PauseReason reason: Int) = handler.request {
        it.pauseImp(force = false, releaseResource = releaseResource, reason = reason)
    }

    private fun pauseImp(force: Boolean = false, @PauseReason reason: Int, releaseResource: Boolean = true) {
        if (audioPlayer.isPlaying || force) {
            audioPlayer.pause()
            pauseReason = reason
            broadcastStopLyric()
            playerState = PlayerState.PAUSED
            if (releaseResource) releaseTakenResources()
        }
    }

    override fun togglePlayPause() = handler.request {
        it.togglePlayPauseImp()
    }

    private fun togglePlayPauseImp() {
        if (audioPlayer.isPlaying) {
            pauseImp(force = false, reason = PauseReason.PAUSE_BY_MANUAL_ACTION)
        } else {
            playImp()
        }
    }

    override val isPlaying get() = audioPlayer.isInitialized && audioPlayer.isPlaying

    /**
     * Jump to beginning of this song
     */
    override fun rewindToBeginning() = handler.request {
        it.rewindToBeginningImp()
    }

    private fun rewindToBeginningImp() {
        seekTo(0)
        service.requireRefreshMediaSessionState()
    }

    /**
     * Return to previous song
     */
    override fun jumpBackward(force: Boolean) = handler.request {
        it.jumpBackwardImp(force)
    }

    private fun jumpBackwardImp(force: Boolean) {
        val position =
            if (force) {
                queueManager.previousLoopPosition
            } else {
                queueManager.previousSongPosition
            }
        playAtImp(position)
    }

    /**
     * [rewindToBeginningImp] or [jumpBackwardImp]
     */
    override fun back(force: Boolean) = handler.request {
        it.backImp(force)
    }

    private fun backImp(force: Boolean) {
        if (audioPlayer.position() > 5000) {
            rewindToBeginningImp()
        } else {
            jumpBackwardImp(force)
        }
    }

    /**
     * Skip and jump to next song
     */
    override fun jumpForward(force: Boolean) = handler.request {
        it.jumpForwardImp(force)
    }

    private fun jumpForwardImp(force: Boolean) {
        val position =
            if (force) {
                queueManager.nextLoopPosition
            } else {
                queueManager.nextSongPosition
            }
        if (position >= 0) {
            playAtImp(position)
        } else {
            pauseImp(force = true, reason = PauseReason.PAUSE_FOR_QUEUE_ENDED)
            observers.executeForEach {
                onReceivingMessage(MSG_NO_MORE_SONGS)
            }
        }
    }

    /**
     * Move current time to [position]
     * @param position time in millisecond
     */
    override fun seekTo(position: Long): Int = synchronized(audioPlayer) {
        seekToImp(position)
    }

    private fun seekToImp(position: Long): Int {
        return audioPlayer.seek(position.toInt())
    }

    override fun stop() = handler.request {
        it.stopImp()
    }

    private fun stopImp() {
        audioPlayer.stop()
        broadcastStopLyric()
        playerState = PlayerState.STOPPED
        releaseTakenResources()
        observers.executeForEach {
            onReceivingMessage(MSG_PLAYER_STOPPED)
        }
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

    var audioDucking: Boolean = true

    override fun onTrackWentToNext() {
        handler.request {
            // check sleep timer
            if (quitAfterFinishCurrentSong) {
                stopImp()
                return@request
            }
            queueManager.moveToNextSong(false)
            notifyNowPlayingChanged()
            prepareNextPlayer(queueManager.nextSong)
        }
    }

    override fun onTrackEnded() {
        // check sleep timer
        if (quitAfterFinishCurrentSong) {
            stop()
            return
        }
        if (queueManager.isQueueEnded()) {
            handler.request {
                pauseImp(force = true, reason = PauseReason.PAUSE_FOR_QUEUE_ENDED)
            }
            broadcastStopLyric()
            observers.executeForEach {
                onReceivingMessage(MSG_NO_MORE_SONGS)
            }
        } else {
            handler.request {
                prepareNextPlayer(queueManager.nextSong)
                queueManager.moveToNextSong(false)
                playAtImp(queueManager.currentSongPosition)
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

    companion object {
        private fun getTrackUri(songId: Long): Uri =
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
    }

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
                pause(releaseResource = true, reason = PauseReason.PAUSE_FOR_AUDIO_BECOMING_NOISY)
            }
        }
    }

    fun saveCurrentMills() = handler.request {
        saveCurrentMillsImp()
    }

    private fun saveCurrentMillsImp() {
        QueuePreferenceManager(service).currentMillisecond = audioPlayer.position()
    }

    val audioSessionId: Int get() = audioPlayer.audioSessionId

    override fun setVolume(vol: Float) = handler.request { playerController ->
        playerController.audioPlayer.setVolume(vol)
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
                    playerController.audioPlayer.setVolume(currentDuckVolume)

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
                    playerController.audioPlayer.setVolume(currentDuckVolume)

                }

                RE_PREPARE_NEXT_PLAYER -> synchronized(playerController.audioPlayer) {
                    playerController.prepareNextPlayer(playerController.queueManager.nextSong)
                }


                CLEAN_NEXT_PLAYER      -> synchronized(playerController.audioPlayer) {
                    playerController.prepareNextPlayer(null)
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

    override val songProgressMillis: Int
        get() = if (audioPlayer.isInitialized) audioPlayer.position() else -1

    override val songDurationMillis: Int
        get() = if (audioPlayer.isInitialized) audioPlayer.duration() else -1

    override var playerSpeed
        get() = audioPlayer.speed
        set(speed) = handler.request {
            audioPlayer.speed = speed
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