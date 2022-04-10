/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.music_service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
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

    init {
        _audioPlayer = AudioPlayer(musicService, this)
    }

    /**
     * release all resource and destroy
     */
    fun destroy() {
        stop()
        _audioPlayer = null
        _service = null
    }

    var playerState: PlayerState = PlayerState.PREPARING
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
        if (result) audioPlayer.setNextDataSource(getTrackUri(next.id).toString())
        // todo update META
        return result
    }

    /**
     * Play songs from a certain position
     */
    fun playFrom(position: Int) {
        if (service.audioFocusManager.requestAudioFocus()) {
            if (audioPlayer.isPlaying()) audioPlayer.pause()
            prepareSong(position)
            audioPlayer.start()
            playerState = PlayerState.PLAYING
            checkNoisyReceiver()
            // todo update
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
        private set

    /**
     * Pause
     */
    fun pause() {
        if (audioPlayer.isPlaying()) {
            audioPlayer.pause()
            playerState = PlayerState.PAUSED
        }
    }

    fun togglePlayPause() {
        if (audioPlayer.isPlaying()) {
            pause()
            pauseReason = PAUSE_BY_MANUAL_ACTION
        } else {
            play()
        }
    }

    fun isPlaying() = audioPlayer.isReady && audioPlayer.isPlaying()
    val currentTimeAxis: Int = if (audioPlayer.isReady) { audioPlayer.processTimeAxis() } else { -1 }

    /**
     * Jump to beginning of this song
     */
    fun rewindToBeginning() {
        audioPlayer.seek(0)
    }

    /**
     * Return to previous song
     */
    fun jumpBackward() {
        playFrom(service.queueManager.nextSongPosition)
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
        playFrom(service.queueManager.previousSongPosition)
    }

    /**
     * Move current time to [position]
     * @param position time in millisecond
     */
    fun seek(position: Long) {
        audioPlayer.seek(position.toInt())
    }

    fun stop() {
        pause()
        playerState = PlayerState.STOPPED
        // todo send message
    }

    private var noisyReceiverRegistered = false
    private fun checkNoisyReceiver() {
        if (!noisyReceiverRegistered) {
            service.registerReceiver(becomingNoisyReceiver, becomingNoisyReceiverIntentFilter)
            noisyReceiverRegistered = true
        }
    }
    private val becomingNoisyReceiverIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
                pauseReason = PAUSE_FOR_AUDIO_BECOMING_NOISY
            }
        }
    }

    override fun onTrackWentToNext() {
        // todo handle queue ended
        audioPlayer.pause()
        prepareSong(service.queueManager.currentSongPosition + 1)
        audioPlayer.start()
    }

    override fun onTrackEnded() {
        audioPlayer.pause()
    }

    companion object {
        const val NOT_PAUSED = 0
        const val PAUSE_BY_MANUAL_ACTION = 2
        const val PAUSE_FOR_QUEUE_ENDED = 4
        const val PAUSE_FOR_AUDIO_BECOMING_NOISY = 8
        const val PAUSE_ERROR = -2
        private fun getTrackUri(songId: Long): Uri = MusicUtil.getSongFileUri(songId)
    }
}
enum class PlayerState {
    PLAYING, PAUSED, STOPPED, PREPARING
}
