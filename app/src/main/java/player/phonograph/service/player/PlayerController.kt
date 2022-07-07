package player.phonograph.service.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.os.PowerManager.WakeLock
import android.util.ArrayMap
import android.util.Log
import android.widget.Toast
import java.lang.ref.WeakReference
import player.phonograph.App
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.misc.LyricsUpdateThread
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.LrcLyrics
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.MusicService
import player.phonograph.util.MusicUtil

// todo sleep timer
/**
 * @author chr_56 & Abou Zeid (kabouzeid) (original author)
 */
class PlayerController(musicService: MusicService) : Playback.PlaybackCallbacks, LyricsUpdateThread.ProgressMillsUpdateCallback {

    private var _service: MusicService? = musicService
    private val service: MusicService get() = _service!!

    private val queueManager = App.instance.queueManager

    private var _audioPlayer: AudioPlayer? = null
    private val audioPlayer: AudioPlayer get() = _audioPlayer!!

    private val wakeLock: WakeLock
    private val audioFocusManager: AudioFocusManager = AudioFocusManager()

    val handler: ControllerHandler
    private var thread: HandlerThread

    private lateinit var lyricsUpdateThread: LyricsUpdateThread

    init {
        _audioPlayer = AudioPlayer(musicService, this)

        wakeLock =
            (App.instance.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    javaClass.name
                ).apply {
                    setReferenceCounted(false)
                }

        // setup handler
        thread = HandlerThread("player_controller_handler_thread")
        thread.start()
        handler = ControllerHandler(this, thread.looper)

        lyricsUpdateThread = LyricsUpdateThread(queueManager.currentSong, this)
        lyricsUpdateThread.start()
    }

    /**
     * release all resource and destroy
     */
    fun destroy() {
        stop()
        unregisterBecomingNoisyReceiver(service)
        _audioPlayer = null
        wakeLock.release()
        thread.quitSafely()
        handler.looper.quitSafely()

        lyricsUpdateThread.currentSong = null
        lyricsUpdateThread.quit()

        _service = null
    }

    fun acquireWakeLock(milli: Long) {
        wakeLock.acquire(milli)
    }

    fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private var _playerState: PlayerState = PlayerState.PREPARING
    var playerState: PlayerState
        get() = _playerState
        private set(value) {
            val oldState = _playerState
            synchronized(_playerState) {
                _playerState = value
            }
            observers.executeForEach { onPlayerStateChanged(oldState, value) }
        }

    /**
     * prepare current and next player and set queue cursor(position)
     * @param position where to start in queue
     * @return true if it is ready
     */
    private fun prepareSongsImp(position: Int): Boolean {
        // todo change STATE
        queueManager.setQueueCursor(position)
        return prepareCurrentPlayer(queueManager.currentSong).also { setCurrentSuccess ->
            if (setCurrentSuccess) prepareNextPlayer(queueManager.nextSong)
            else prepareNextPlayer(null)
        }
        // todo update META
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
            Toast.makeText(
                service,
                service.resources.getString(R.string.unplayable_file),
                Toast.LENGTH_SHORT
            ).show()
            jumpForwardImp(false)
        }
        lyricsUpdateThread.currentSong = queueManager.getSongAt(position)
        log("playAtImp", "current: at $position song(${queueManager.currentSong})")
    }

    /**
     * continue play
     */
    fun play() = handler.request {
        it.playImp()
    }
    private fun playImp() {
        if (queueManager.playingQueue.isNotEmpty()) {
            if (audioFocusManager.requestAudioFocus()) {
                checkAndRegisterBecomingNoisyReceiver(service)
                if (!audioPlayer.isPlaying()) {
                    // Actual Logics Start
                    if (!audioPlayer.isInitialized) {
                        playAtImp(queueManager.currentSongPosition)
                    } else {
                        audioPlayer.start()
                        log("playImp", "start playing: ${queueManager.currentSong.title}")

                        playerState = PlayerState.PLAYING
                        pauseReason = NOT_PAUSED
                        // todo notify META
                        acquireWakeLock(
                            queueManager.currentSong.duration - audioPlayer.position() + 1000L
                        )
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
            pauseImp(true)
        }
    }

    var pauseReason: Int = NOT_PAUSED

    /**
     * Pause
     */
    fun pause() = handler.request {
        it.pauseImp()
    }
    private fun pauseImp(force: Boolean = false) {
        if (audioPlayer.isPlaying() || force) {
            audioPlayer.pause()
            playerState = PlayerState.PAUSED
            releaseWakeLock()
        }
    }

    fun togglePlayPause() = handler.request {
        it.togglePlayPauseImp()
    }
    private fun togglePlayPauseImp() {
        if (audioPlayer.isPlaying()) {
            pauseImp()
            pauseReason = PAUSE_BY_MANUAL_ACTION
        } else {
            playImp()
        }
    }

    fun isPlaying() = audioPlayer.isInitialized && audioPlayer.isPlaying()

    val positionInTrack: Int =
        if (audioPlayer.isInitialized) {
            audioPlayer.position()
        } else {
            -1
        }

    /**
     * Jump to beginning of this song
     */
    fun rewindToBeginning() = handler.request {
        it.rewindToBeginningImp()
    }
    private fun rewindToBeginningImp() {
        audioPlayer.seek(0)
    }

    /**
     * Return to previous song
     */
    fun jumpBackward(force: Boolean) = handler.request {
        it.jumpBackwardImp(force)
    }
    private fun jumpBackwardImp(force: Boolean) {
        if (force) {
            playAtImp(queueManager.previousSongPositionInList)
        } else {
            playAtImp(queueManager.previousSongPosition)
        }
    }

    /**
     * [rewindToBeginningImp] or [jumpBackwardImp]
     */
    fun back(force: Boolean) = handler.request {
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
    fun jumpForward(force: Boolean) = handler.request {
        it.jumpForwardImp(force)
    }
    private fun jumpForwardImp(force: Boolean) {
        if (force) {
            playAtImp(queueManager.nextSongPositionInList)
        } else {
            if (!queueManager.lastTrack) {
                playAtImp(queueManager.nextSongPosition)
            } else {
                pauseImp(true)
                // todo notify queue ended
            }
        }
    }

    /**
     * Move current time to [position]
     * @param position time in millisecond
     */
    fun seekTo(position: Long) = handler.request {
        it.seekToImp(position)
    }
    private fun seekToImp(position: Long) {
        audioPlayer.seek(position.toInt())
    }

    fun stop() = handler.request {
        it.stopImp()
    }
    private fun stopImp() {
        pauseImp(true)
        playerState = PlayerState.STOPPED
        // todo send message
    }

    override fun onTrackWentToNext() {
        handler.request {
            queueManager.moveToNextSong()
            prepareNextPlayer(queueManager.nextSong)
        }
    }

    override fun onTrackEnded() {
        handler.request {
            pauseImp(true)
        }
    }

    companion object {
        const val NOT_PAUSED = 0
        const val PAUSE_BY_MANUAL_ACTION = 2
        const val PAUSE_FOR_QUEUE_ENDED = 4
        const val PAUSE_FOR_AUDIO_BECOMING_NOISY = 8
        const val PAUSE_ERROR = -2

        private fun getTrackUri(songId: Long): Uri = MusicUtil.getSongFileUri(songId)
    }

    private var becomingNoisyReceiverRegistered = false
    private fun checkAndRegisterBecomingNoisyReceiver(context: Context) {
        if (!becomingNoisyReceiverRegistered) {
            context.registerReceiver(
                becomingNoisyReceiver,
                IntentFilter(
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY
                )
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
                pause()
            }
        }
    }

    private val observers: MutableList<PlayerStateObserver> = ArrayList()
    fun addObserver(observer: PlayerStateObserver) = observers.add(observer)
    fun removeObserver(observer: PlayerStateObserver): Boolean = observers.remove(observer)

    class ControllerHandler(playerController: PlayerController, looper: Looper) : Handler(looper) {
        private val controllerRef: WeakReference<PlayerController> = WeakReference(playerController)

        private var requestIdCumulator = 0
        private val requestList: ArrayMap<Int, RunnableRequest> = ArrayMap(1)

        /**
         * Request running in the handler thread
         * @param request RunnableRequest: (PlayerController) -> Unit
         */
        fun request(request: RunnableRequest) {
            val requestId = requestIdCumulator++
            synchronized(requestList) {
                requestList[requestId] = request
                sendMessage(
                    Message.obtain(this, HANDLER_EXECUTE_REQUEST, requestId, 0)
                )
            }
        }

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                HANDLER_EXECUTE_REQUEST -> {
                    val requestId = msg.arg1
                    val controller = controllerRef.get()
                    if (controller == null) {
                        ErrorNotification.postErrorNotification(
                            "ControllerHandler: weak reference for PlayerController is missing!\n${Thread.currentThread().stackTrace}"
                        )
                        return
                    }
                    requestList[requestId]?.let { runnableRequest ->
                        runnableRequest.invoke(controller)
                        synchronized(requestList) {
                            requestList.remove(requestId)
                        }
                    }
                }
            }
        }

        companion object {
            // MSG WHAT
            private const val HANDLER_EXECUTE_REQUEST = 10
        }
    }

    /**
     * API for "StatusBar Lyrics" Xposed Module
     */
    override fun getProgressTimeMills(): Int = audioPlayer.position()
    override fun isRunning(): Boolean = isPlaying()
    private fun broadcastStopLyric() = App.instance.lyricsService.stopLyric()
    fun replaceLyrics(lyrics: LrcLyrics?) {
        if (lyrics != null) {
            lyricsUpdateThread.forceReplaceLyrics(lyrics)
        } else {
            lyricsUpdateThread.currentSong = null
        }
    }

    fun log(where: String, msg: String, force: Boolean = false) {
        if (DEBUG || force) Log.d("PlayerController", "@$where ※$msg")
    }
}
