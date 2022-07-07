package player.phonograph.service.player

import android.content.Context
import android.net.Uri
import android.os.*
import android.os.PowerManager.WakeLock
import android.util.ArrayMap
import android.widget.Toast
import java.lang.ref.WeakReference
import player.phonograph.App
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
     * prepare player and set queue cursor(position)
     * @param position where to start in queue
     * @return true if it is ready
     */
    private fun prepareSongImp(position: Int): Boolean {
        // todo change STATE
        queueManager.setQueueCursor(position)
        val current = queueManager.currentSong
        val next = queueManager.nextSong
        if (current == Song.EMPTY_SONG) return false
        val result = audioPlayer.setDataSource(getTrackUri(current.id).toString())
        // prepare next
        if (next != Song.EMPTY_SONG) {
            if (result) audioPlayer.setNextDataSource(getTrackUri(next.id).toString())
        } else {
            audioPlayer.setNextDataSource(null)
        }
        // todo update META
        lyricsUpdateThread.currentSong = queueManager.getSongAt(position)
        return result
    }

    /**
     * Play songs from a certain position
     */
    fun playAt(position: Int) = handler.request {
        it.playFrom(position)
    }
    private fun playFrom(position: Int) {
        if (audioFocusManager.requestAudioFocus()) {
            if (audioPlayer.isPlaying()) audioPlayer.pause()
            prepareSongImp(position)
            audioPlayer.start()
            playerState = PlayerState.PLAYING
            // todo update
            acquireWakeLock(
                queueManager.currentSong.duration - audioPlayer.position() + 1000L
            )
            lyricsUpdateThread.currentSong = queueManager.getSongAt(position)
        } else {
            Toast.makeText(
                service,
                service.resources.getString(R.string.audio_focus_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * continue play
     */
    fun play() = handler.request {
        it.playImp()
    }
    private fun playImp() {
        pauseReason = NOT_PAUSED
        if (queueManager.playingQueue.isNotEmpty()) {
            playFrom(queueManager.currentSongPosition)
        } else {
            // todo
        }
    }

    var pauseReason: Int = NOT_PAUSED

    /**
     * Pause
     */
    fun pause() = handler.request {
        it.pauseImp()
    }
    private fun pauseImp() {
        if (audioPlayer.isPlaying()) {
            audioPlayer.pause()
            playerState = PlayerState.PAUSED
        }
        releaseWakeLock()
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
    fun jumpBackward() = handler.request {
        it.jumpBackwardImp()
    }
    private fun jumpBackwardImp() {
        playFrom(queueManager.previousSongPosition)
    }

    /**
     * [rewindToBeginningImp] or [jumpBackwardImp]
     */
    fun back() = handler.request {
        it.backImp()
    }
    private fun backImp() {
        if (audioPlayer.position() > 5000) {
            rewindToBeginningImp()
        } else {
            jumpBackwardImp()
        }
    }

    /**
     * Skip and jump to next song
     */
    fun jumpForward() = handler.request {
        it.jumpForwardImp()
    }
    private fun jumpForwardImp() {
        if (!queueManager.lastTrack) {
            playFrom(queueManager.nextSongPosition)
        } else {
            // todo send message
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
        pauseImp()
        playerState = PlayerState.STOPPED
        // todo send message
    }

    // todo rename callback
    override fun onTrackWentToNext() {
        handler.request {
            pauseImp()
            if (!queueManager.lastTrack) {
                playFrom(queueManager.currentSongPosition + 1)
            }
        }
    }

    override fun onTrackEnded() {
        handler.request {
            pauseImp()
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
}
