/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.music_service

import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.widget.Toast
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.util.MusicUtil

// todo lyrics
// todo sleep timer
class PlayerController(musicService: MusicService) : Playback.PlaybackCallbacks {
    private var _service: MusicService? = musicService
    private val service: MusicService get() = _service!!

    private var _audioPlayer: AudioPlayer? = null
    private val audioPlayer: AudioPlayer get() = _audioPlayer!!

    private var _handler: MessageHandler? = null
    val handler: Handler get() = _handler!!
    private var thread: HandlerThread? = null

    init {
        _audioPlayer = AudioPlayer(musicService, this)

        thread = HandlerThread("player_controller_handler_thread")
        thread!!.start()
        _handler = MessageHandler(thread!!.looper)
    }

    /**
     * release all resource and destroy
     */
    fun destroy() {
        stop()
        _audioPlayer = null
        _service = null
        thread?.quitSafely()
        _handler?.looper?.quitSafely()
        _handler = null
    }

    var playerState: PlayerState = PlayerState.PREPARING
        @Synchronized
        private set
    // todo observer model

    /**
     * prepare player and set queue cursor(position)
     * @param position where to start in queue
     * @return true if it is ready
     */
    private fun prepareSong(position: Int): Boolean {
        service.queueManager.setQueueCursor(position)
        val current = service.queueManager.currentSong
        val next = service.queueManager.nextSong
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
        if (service.audioFocusManager.requestAudioFocus()) {
            handler.post {
                if (audioPlayer.isPlaying()) audioPlayer.pause()
                prepareSong(position)
                audioPlayer.start()
                playerState = PlayerState.PLAYING
                // todo update
            }
        } else {
            Toast.makeText(service, service.resources.getString(R.string.audio_focus_denied), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * continue play
     */
    fun play() {
        pauseReason = NOT_PAUSED
        if (service.queueManager.playingQueue.isNotEmpty()) {
            playFrom(service.queueManager.currentSongPosition)
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

    fun isPlaying() = audioPlayer.isReady && audioPlayer.isPlaying()
    val currentTimeAxis: Int = if (audioPlayer.isReady) {
        audioPlayer.processTimeAxis()
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
        playFrom(service.queueManager.previousSongPosition)
    }

    /**
     * [rewindToBeginning] or [jumpBackward]
     */
    fun back() {
        if (audioPlayer.processTimeAxis() > 5000) {
            rewindToBeginning()
        } else {
            jumpBackward()
        }
    }

    /**
     * Skip and jump to next song
     */
    fun jumpForward() {
        if (!service.queueManager.lastTrack) {
            playFrom(service.queueManager.nextSongPosition)
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
        playerState = PlayerState.STOPPED
        // todo send message
    }

    override fun onTrackWentToNext() {
        handler.post {
            audioPlayer.pause()
            if (!service.queueManager.lastTrack) {
                prepareSong(service.queueManager.currentSongPosition + 1)
                audioPlayer.start()
            }
        }
    }

    override fun onTrackEnded() {
        handler.post {
            audioPlayer.pause()
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

    inner class MessageHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
            }
        }
    }
}

enum class PlayerState {
    PLAYING, PAUSED, STOPPED, PREPARING
}
