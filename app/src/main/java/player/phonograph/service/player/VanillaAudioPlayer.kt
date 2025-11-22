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
class VanillaAudioPlayer(private val context: Context, override var gaplessPlayback: Boolean) :
        Playback, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private var currentMediaPlayer = MediaPlayer().also { player ->
        player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
    }

    override var currentDataSource: String = ""
        private set

    private var nextMediaPlayer: MediaPlayer? = null

    override var callbacks: Playback.PlaybackCallbacks? = null

    override var isInitialized: Boolean = false
        private set

    constructor(context: Context, gaplessPlayback: Boolean, callbacks: Playback.PlaybackCallbacks) :
            this(context, gaplessPlayback) {
        this.callbacks = callbacks
    }


    override fun setDataSource(path: String): Boolean {
        isInitialized = false
        isInitialized = setDataSourceImpl(currentMediaPlayer, path)
        if (isInitialized) {
            setNextDataSource(null)
        }
        return isInitialized
    }

    private fun setDataSourceImpl(player: MediaPlayer, path: String): Boolean {
        try {
            player.reset()
            player.setOnPreparedListener(null)

            if (path.startsWith("content://")) {
                player.setDataSource(context, Uri.parse(path))
            } else {
                player.setDataSource(path)
            }

            currentDataSource = path

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
            nextMediaPlayer = MediaPlayer().also { player ->
                player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                if (isInitialized) player.audioSessionId = currentMediaPlayer.audioSessionId
            }

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

    override fun play(): Boolean =
        try {
            // currentMediaPlayer.start()
            playWithSpeed(currentMediaPlayer, _speed)
        } catch (e: IllegalStateException) {
            false
        }

    override fun stop() {
        currentMediaPlayer.reset()
        isInitialized = false
    }

    override fun release() {
        stop()
        currentMediaPlayer.release()
        nextMediaPlayer?.release()
    }


    override fun pause(): Boolean =
        try {
            currentMediaPlayer.pause()
            true
        } catch (e: IllegalStateException) {
            false
        }


    override val isPlaying: Boolean
        get() = isInitialized && currentMediaPlayer.isPlaying


    override fun duration(): Int =
        if (!isInitialized) -1
        else {
            try {
                currentMediaPlayer.duration
            } catch (e: IllegalStateException) {
                -1
            }
        }


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

    override var speed: Float
        get() = _speed
        set(value) {
            if (isPlaying) {
                playWithSpeed(currentMediaPlayer, value)
            } else {
                _speed = value
            }
        }

    private fun playWithSpeed(player: MediaPlayer, targetSpeed: Float): Boolean = try {
        player.playbackParams = PlaybackParams().apply {
            allowDefaults()
            audioFallbackMode = AUDIO_FALLBACK_MODE_MUTE
            speed = targetSpeed
            val outRanged = targetSpeed !in (0.5f..2.0f)
            pitch = if (outRanged) ((targetSpeed - 1.0f) * 0.333f + 1.0f) else 1.0f
        }
        player.start()
        _speed = targetSpeed
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to adjust speed to $targetSpeed", e)
        _speed = 1.0f
        false
    }

    override val audioSessionId: Int get() = currentMediaPlayer.audioSessionId


    override fun setAudioSessionId(sessionId: Int): Boolean =
        try {
            currentMediaPlayer.audioSessionId = sessionId
            true
        } catch (e: IllegalArgumentException) {
            false
        } catch (e: IllegalStateException) {
            false
        }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        isInitialized = false
        currentMediaPlayer.release()
        currentMediaPlayer = MediaPlayer()
        currentMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        callbacks?.onError(what, extra)
        return false
    }

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
        private const val TAG = "VanillaAudioPlayer"
    }
}
