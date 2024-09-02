package player.phonograph.service.player

/**
 * @author chr_56 & Karim Abou Zeid (kabouzeid) (original author)
 */
interface Playback {


    /**
     * @param path The path of the file, or the http/rtsp URL of the stream you want to play
     * @return True if the `player` has been prepared and is ready to play, false otherwise
     */
    fun setDataSource(path: String): Boolean

    /**
     * Set the MediaPlayer to start when this MediaPlayer finishes playback.
     *
     * @param path The path of the file, or the http/rtsp URL of the stream you want to play
     */
    fun setNextDataSource(path: String?)

    /**
     * path of current Data Source
     */
    val currentDataSource: String


    /**
     * Callbacks
     */
    var callbacks: PlaybackCallbacks?

    /**
     * Check if it is prepared
     */
    val isInitialized: Boolean

    /**
     * Starts or resumes
     */
    fun play(): Boolean

    /**
     * Reset to its uninitialized state.
     */
    fun stop()

    /**
     * Releases resources associated with this.
     */
    fun release()

    /**
     * Pause
     */
    fun pause(): Boolean

    /**
     * Checks whether it is playing.
     */
    val isPlaying: Boolean

    /**
     * Gets the duration of the file.
     *
     * @return The duration in milliseconds
     */
    fun duration(): Int

    /**
     * Gets the current playback position.
     *
     * @return The current position in milliseconds
     */
    fun position(): Int

    /**
     * Gets the current playback position.
     *
     * @param whereto The offset in milliseconds from the start to seek to
     * @return The offset in milliseconds from the start to seek to
     */
    fun seek(whereto: Int): Int

    /**
     * Set volume of output
     */
    fun setVolume(vol: Float): Boolean

    /**
     * audio session ID
     */
    val audioSessionId: Int

    /**
     * Sets the audio session ID.
     *
     * @param sessionId The audio session ID
     * @return success or not
     */
    fun setAudioSessionId(sessionId: Int): Boolean

    /**
     * Playback Speed
     */
    var speed: Float

    /**
     * Gapless playback
     */
    var gaplessPlayback: Boolean

    interface PlaybackCallbacks {
        fun onTrackWentToNext()
        fun onTrackEnded()
        fun onError(what: Int, extra: Int)
    }
}
