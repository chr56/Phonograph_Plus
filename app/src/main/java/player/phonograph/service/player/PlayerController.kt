package player.phonograph.service.player

import android.content.Context
import android.net.Uri
import android.os.*
import android.os.PowerManager.WakeLock
import android.widget.Toast
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.service.MusicService
import player.phonograph.util.MusicUtil

// todo lyrics
// todo sleep timer
/**
 * @author chr_56 & Abou Zeid (kabouzeid) (original author)
 */
class PlayerController(musicService: MusicService) : Playback.PlaybackCallbacks {

    private var _service: MusicService? = musicService
    private val service: MusicService get() = _service!!

    private val queueManager = App.instance.queueManager

    private var _audioPlayer: AudioPlayer? = null
    private val audioPlayer: AudioPlayer get() = _audioPlayer!!

    private val wakeLock: WakeLock
    private val audioFocusManager: AudioFocusManager = AudioFocusManager()

    var handler: Handler
    private var thread: HandlerThread

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
        handler = MessageHandler(thread.looper)
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

    var playerState: PlayerState = PlayerState.PREPARING
        @Synchronized
        private set(value) {
            val old = field
            field = value
            observers.executeForEach { onPlayerStateChanged(old, value) }
        }

    /**
     * prepare player and set queue cursor(position)
     * @param position where to start in queue
     * @return true if it is ready
     */
    private fun prepareSong(position: Int): Boolean {
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
        return result
    }

    /**
     * Play songs from a certain position
     */
    fun playFrom(position: Int) {
        if (audioFocusManager.requestAudioFocus()) {
            handler.post {
                if (audioPlayer.isPlaying()) audioPlayer.pause()
                prepareSong(position)
                audioPlayer.start()
                playerState = PlayerState.PLAYING
                // todo update
                acquireWakeLock(
                    queueManager.currentSong.duration - audioPlayer.position() + 1000L
                )
            }
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
    fun play() {
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
    fun pause() {
        handler.post {
            if (audioPlayer.isPlaying()) {
                audioPlayer.pause()
                playerState = PlayerState.PAUSED
            }
            service.releaseWakeLock()
        }
    }

    fun togglePlayPause() {
        handler.post {
            if (audioPlayer.isPlaying()) {
                pause()
                pauseReason = PAUSE_BY_MANUAL_ACTION
            } else {
                play()
            }
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
    fun rewindToBeginning() {
        handler.post {
            audioPlayer.seek(0)
        }
    }

    /**
     * Return to previous song
     */
    fun jumpBackward() {
        playFrom(queueManager.previousSongPosition)
    }

    /**
     * [rewindToBeginning] or [jumpBackward]
     */
    fun back() {
        if (audioPlayer.position() > 5000) {
            rewindToBeginning()
        } else {
            jumpBackward()
        }
    }

    /**
     * Skip and jump to next song
     */
    fun jumpForward() {
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
    fun seek(position: Long) {
        handler.post {
            audioPlayer.seek(position.toInt())
        }
    }

    fun stop() {
        pause()
        handler.post { playerState = PlayerState.STOPPED }
        // todo send message
    }

    // todo rename callback
    override fun onTrackWentToNext() {
        pause()
        if (!queueManager.lastTrack) {
            playFrom(queueManager.currentSongPosition + 1)
        }
    }

    override fun onTrackEnded() {
        pause()
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

    inner class MessageHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
            }
        }
    }
}
