package player.phonograph.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import player.phonograph.R
import player.phonograph.service.playback.Playback
import player.phonograph.service.playback.Playback.PlaybackCallbacks
import player.phonograph.util.PreferenceUtil.Companion.getInstance

/**
 * @author Andrew Neal, Karim Abou Zeid (kabouzeid)
 */
class MultiPlayer(private val context: Context?) :
    Playback,
    MediaPlayer.OnErrorListener,
    OnCompletionListener {

    private var mCurrentMediaPlayer = MediaPlayer()
    private var mNextMediaPlayer: MediaPlayer? = null
    private var callbacks: PlaybackCallbacks? = null
    private var mIsInitialized = false

    init {
        mCurrentMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
    }

    /**
     * @param path The path of the file, or the http/rtsp URL of the stream
     * you want to play
     * @return True if the `player` has been prepared and is
     * ready to play, false otherwise
     */
    override fun setDataSource(path: String): Boolean {
        mIsInitialized = false
        mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path)
        if (mIsInitialized) {
            setNextDataSource(null)
        }
        return mIsInitialized
    }

    /**
     * @param player The [MediaPlayer] to use
     * @param path   The path of the file, or the http/rtsp URL of the stream
     * you want to play
     * @return True if the `player` has been prepared and is
     * ready to play, false otherwise
     */
    private fun setDataSourceImpl(player: MediaPlayer, path: String): Boolean {
        if (context == null) {
            return false
        }
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            this.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL)
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
        if (context == null) {
            return
        }
        try {
            mCurrentMediaPlayer.setNextMediaPlayer(null)
        } catch (e: IllegalArgumentException) {
            Log.i(TAG, "Next media player is current one, continuing")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Media player not initialized!")
            return
        }
        mNextMediaPlayer?.let {
            it.release()
            mNextMediaPlayer = null
        }
        if (path == null) {
            return
        }
        if (getInstance(context).gaplessPlayback()) {
            mNextMediaPlayer = MediaPlayer()
            mNextMediaPlayer!!.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            mNextMediaPlayer!!.audioSessionId = audioSessionId
            if (setDataSourceImpl(mNextMediaPlayer!!, path)) {
                try {
                    mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "setNextDataSource: setNextMediaPlayer()", e)
                    mNextMediaPlayer?.let {
                        it.release()
                        mNextMediaPlayer = null
                    }
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "setNextDataSource: setNextMediaPlayer()", e)
                    mNextMediaPlayer?.let {
                        it.release()
                        mNextMediaPlayer = null
                    }
                }
            } else {
                mNextMediaPlayer?.let {
                    it.release()
                    mNextMediaPlayer = null
                }
            }
        }
    }

    /**
     * Sets the callbacks
     *
     * @param callbacks The callbacks to use
     */
    override fun setCallbacks(callbacks: PlaybackCallbacks?) {
        this.callbacks = callbacks
    }

    /**
     * @return True if the player is ready to go, false otherwise
     */
    override fun isInitialized(): Boolean {
        return mIsInitialized
    }

    /**
     * Starts or resumes playback.
     */
    override fun start(): Boolean {
        return try {
            mCurrentMediaPlayer.start()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * Resets the MediaPlayer to its uninitialized state.
     */
    override fun stop() {
        mCurrentMediaPlayer.reset()
        mIsInitialized = false
    }

    /**
     * Releases resources associated with this MediaPlayer object.
     */
    override fun release() {
        stop()
        mCurrentMediaPlayer.release()
        mNextMediaPlayer?.release()
    }

    /**
     * Pauses playback. Call start() to resume.
     */
    override fun pause(): Boolean {
        return try {
            mCurrentMediaPlayer.pause()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * Checks whether the MultiPlayer is playing.
     */
    override fun isPlaying(): Boolean {
        return mIsInitialized && mCurrentMediaPlayer.isPlaying
    }

    /**
     * Gets the duration of the file.
     *
     * @return The duration in milliseconds
     */
    override fun duration(): Int {
        return if (!mIsInitialized) {
            -1
        } else try {
            mCurrentMediaPlayer.duration
        } catch (e: IllegalStateException) {
            -1
        }
    }

    /**
     * Gets the current playback position.
     *
     * @return The current position in milliseconds
     */
    override fun position(): Int {
        return if (!mIsInitialized) {
            -1
        } else try {
            mCurrentMediaPlayer.currentPosition
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
    override fun seek(whereto: Int): Int {
        return try {
            mCurrentMediaPlayer.seekTo(whereto)
            whereto
        } catch (e: IllegalStateException) {
            -1
        }
    }

    override fun setVolume(vol: Float): Boolean {
        return try {
            mCurrentMediaPlayer.setVolume(vol, vol)
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * Sets the audio session ID.
     *
     * @param sessionId The audio session ID
     */
    override fun setAudioSessionId(sessionId: Int): Boolean {
        return try {
            mCurrentMediaPlayer.audioSessionId = sessionId
            true
        } catch (e: IllegalArgumentException) {
            false
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * Returns the audio session ID.
     *
     * @return The current audio session ID.
     */
    override fun getAudioSessionId(): Int {
        return mCurrentMediaPlayer.audioSessionId
    }

    /**
     * {@inheritDoc}
     */
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        mIsInitialized = false
        mCurrentMediaPlayer.release()
        mCurrentMediaPlayer = MediaPlayer()
        mCurrentMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        if (context != null) {
            Toast.makeText(
                context,
                context.resources.getString(R.string.unplayable_file),
                Toast.LENGTH_SHORT
            ).show()
        }
        return false
    }

    /**
     * {@inheritDoc}
     */
    override fun onCompletion(mp: MediaPlayer) {
        if (mp === mCurrentMediaPlayer && mNextMediaPlayer != null) {
            mIsInitialized = false
            mCurrentMediaPlayer.release()
            mCurrentMediaPlayer = mNextMediaPlayer!!
            mIsInitialized = true
            mNextMediaPlayer = null
            callbacks?.onTrackWentToNext()
        } else {
            callbacks?.onTrackEnded()
        }
    }

    @Suppress("PropertyName")
    val TAG: String = MultiPlayer::class.java.simpleName
}
