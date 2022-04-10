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
class PlayerController(musicService: MusicService) : Playback.PlaybackCallbacks {
    private var _service: MusicService? = musicService
    private val service: MusicService get() = _service!!

    private var _audioPlayer: AudioPlayer? = null
    private val audioPlayer: AudioPlayer get() = _audioPlayer!!

    init {
        _audioPlayer = AudioPlayer(musicService, this)
    }

    fun destroy() {
        _audioPlayer = null
        _service = null
    }

    var playerState: PlayerState = PlayerState.PREPARING
        private set
    // todo observer model

    /**
     * @param position where to start in queue
     * @return true if it is ready
     */
    private fun prepareSong(position: Int): Boolean {
        service.queueManager.setQueueCursor(position)
        val current = service.queueManager.getCurrentSong()
        val next = service.queueManager.getNextSong()
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
            playFrom(service.queueManager.currentQueueCursor)
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
        playFrom(service.queueManager.nextSongCursor)
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
        playFrom(service.queueManager.previousSongCursor)
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
        TODO("Not yet implemented")
    }

    override fun onTrackEnded() {
        TODO("Not yet implemented")
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
