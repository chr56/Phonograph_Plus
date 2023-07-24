package player.phonograph.service.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.PlaybackParams.AUDIO_FALLBACK_MODE_MUTE
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log

/**
 * @author chr_56, Andrew Neal, Karim Abou Zeid (kabouzeid)
 */
class AudioPlayer(private val context: Context, var gaplessPlayback: Boolean) :
        Playback, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private var currentMediaPlayer = MediaPlayer().also {
        it.setWakeMode(
            context,
            PowerManager.PARTIAL_WAKE_LOCK
        )
    }
    private var nextMediaPlayer: MediaPlayer? = null

    private var callbacks: Playback.PlaybackCallbacks? = null

    override var isInitialized: Boolean = false
        private set

    /**
     * Sets the callbacks
     * @param callbacks The callbacks to use
     */
    override fun setCallbacks(callbacks: Playback.PlaybackCallbacks?) {
        this.callbacks = callbacks
    }

    constructor(context: Context, gaplessPlayback: Boolean, callbacks: Playback.PlaybackCallbacks) :
            this(context, gaplessPlayback) {
        this.callbacks = callbacks
    }

    /**
     * @param path The path of the file, or the http/rtsp URL of the stream
     * you want to play
     * @return True if the `player` has been prepared and is
     * ready to play, false otherwise
     */
    override fun setDataSource(path: String): Boolean {
        isInitialized = false
        isInitialized = setDataSourceImpl(currentMediaPlayer, path)
        if (isInitialized) {
            setNextDataSource(null)
        }
        return isInitialized
    }

    /**
     * @param player The [MediaPlayer] to use
     * @param path   The path of the file, or the http/rtsp URL of the stream
     * you want to play
     * @return True if the `player` has been prepared and is
     * ready to play, false otherwise
     */
    private fun setDataSourceImpl(player: MediaPlayer, path: String): Boolean {
        try {
            player.reset()
            player.setOnPreparedListener(null)

            if (path.startsWith("content://")) {
                player.setDataSource(context, Uri.parse(path))
            } else {
                player.setDataSource(path)
            }

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            this.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL)
                        }
                    }
                    .build()
            )

            player.prepare()
        } catch (e: Exception) {
            return false
        }
        player.setOnCompletionListener(this)
        player.setOnErrorListener(this)

        val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        context.sendBroadcast(intent)

        return true
    }

    /**
     * Set the MediaPlayer to start when this MediaPlayer finishes playback.
     *
     * @param path The path of the file, or the http/rtsp URL of the stream
     * you want to play
     */
    override fun setNextDataSource(path: String?) {
        try {
            currentMediaPlayer.setNextMediaPlayer(null)
        } catch (e: IllegalArgumentException) {
            Log.i(TAG, "Next media player is current one, continuing")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Media player not initialized!")
            return
        }

        nextMediaPlayer?.let {
            it.release()
            nextMediaPlayer = null
        }
        if (path == null) {
            return
        }

        if (gaplessPlayback) {
            nextMediaPlayer = MediaPlayer()
            nextMediaPlayer!!.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            nextMediaPlayer!!.audioSessionId = audioSessionId

            if (setDataSourceImpl(nextMediaPlayer!!, path)) {
                try {
                    currentMediaPlayer.setNextMediaPlayer(nextMediaPlayer)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "setNextDataSource: setNextMediaPlayer()", e)
                    nextMediaPlayer?.let {
                        it.release()
                        nextMediaPlayer = null
                    }
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "setNextDataSource: setNextMediaPlayer()", e)
                    nextMediaPlayer?.let {
                        it.release()
                        nextMediaPlayer = null
                    }
                }
            } else {
                nextMediaPlayer?.let {
                    it.release()
                    nextMediaPlayer = null
                }
            }
        }
    }

    /**
     * Starts or resumes playback.
     */
    override fun start(): Boolean =
        try {
            currentMediaPlayer.start()
            applySpeed(currentMediaPlayer, _speed)
            true
        } catch (e: IllegalStateException) {
            false
        }

    /**
     * Resets the MediaPlayer to its uninitialized state.
     */
    override fun stop() {
        currentMediaPlayer.reset()
        isInitialized = false
    }

    /**
     * Releases resources associated with this MediaPlayer object.
     */
    override fun release() {
        stop()
        currentMediaPlayer.release()
        nextMediaPlayer?.release()
    }

    /**
     * Pauses playback. Call start() to resume.
     */
    override fun pause(): Boolean =
        try {
            currentMediaPlayer.pause()
            true
        } catch (e: IllegalStateException) {
            false
        }

    /**
     * Checks whether the MultiPlayer is playing.
     */
    override fun isPlaying(): Boolean = isInitialized && currentMediaPlayer.isPlaying

    /**
     * Gets the duration of the file.
     *
     * @return The duration in milliseconds
     */
    override fun duration(): Int =
        if (!isInitialized) -1
        else {
            try {
                currentMediaPlayer.duration
            } catch (e: IllegalStateException) {
                -1
            }
        }

    /**
     * Gets the current playback position.
     *
     * @return The current position in milliseconds
     */
    override fun position(): Int =
        if (!isInitialized) {
            -1
        } else {
            try {
                currentMediaPlayer.currentPosition
            } catch (e: IllegalStateException) {
                -1
            }
        }

    /**
     * Gets the current playback position.
     *
     * @param whereto The offset in milliseconds from the start to seek to
     * @return The offset in milliseconds from the start to seek to
     */
    override fun seek(whereto: Int): Int =
        try {
            currentMediaPlayer.seekTo(whereto)
            whereto
        } catch (e: IllegalStateException) {
            -1
        }

    override fun setVolume(vol: Float): Boolean =
        try {
            currentMediaPlayer.setVolume(vol, vol)
            true
        } catch (e: IllegalStateException) {
            false
        }

    private var _speed: Float = 1.0f

    var speed: Float
        get() = _speed
        set(value) {
            _speed = value
            // applySpeed(currentMediaPlayer, _speed)
        }

    private fun applySpeed(player: MediaPlayer, targetSpeed: Float) {
        player.playbackParams = PlaybackParams().apply {
            allowDefaults()
            audioFallbackMode = AUDIO_FALLBACK_MODE_MUTE
            speed = targetSpeed
            val outRanged = targetSpeed !in (0.5f..2.0f)
            pitch = if (outRanged) targetSpeed else 1.0f
        }
    }

    override val audioSessionId: Int get() = currentMediaPlayer.audioSessionId

    /**
     * Sets the audio session ID.
     *
     * @param sessionId The audio session ID
     * @return success or not
     */
    override fun setAudioSessionId(sessionId: Int): Boolean =
        try {
            currentMediaPlayer.audioSessionId = sessionId
            true
        } catch (e: IllegalArgumentException) {
            false
        } catch (e: IllegalStateException) {
            false
        }

    /**
     * {@inheritDoc}
     */
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        isInitialized = false
        currentMediaPlayer.release()
        currentMediaPlayer = MediaPlayer()
        currentMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        callbacks?.onError(what, extra)
        return false
    }

    /**
     * {@inheritDoc}
     */
    override fun onCompletion(mp: MediaPlayer) {
        if (mp === currentMediaPlayer && nextMediaPlayer != null) {
            isInitialized = false
            currentMediaPlayer.release()
            currentMediaPlayer = nextMediaPlayer!!
            isInitialized = true
            nextMediaPlayer = null
            callbacks?.onTrackWentToNext()
        } else {
            callbacks?.onTrackEnded()
        }
    }

    companion object {
        private const val TAG = "Player"
    }
}
