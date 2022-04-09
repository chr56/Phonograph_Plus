package player.phonograph.service.playback

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
interface Playback {
    fun setDataSource(path: String): Boolean
    fun setNextDataSource(path: String?)
    fun setCallbacks(callbacks: PlaybackCallbacks?)
    fun isInitialized(): Boolean
    fun start(): Boolean
    fun stop()
    fun release()
    fun pause(): Boolean
    fun isPlaying(): Boolean
    fun duration(): Int
    fun position(): Int
    fun seek(whereto: Int): Int
    fun setVolume(vol: Float): Boolean
    fun setAudioSessionId(sessionId: Int): Boolean
    val audioSessionId: Int

    interface PlaybackCallbacks {
        fun onTrackWentToNext()
        fun onTrackEnded()
    }
}