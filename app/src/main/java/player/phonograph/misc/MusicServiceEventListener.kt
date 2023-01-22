package player.phonograph.misc

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
interface MusicServiceEventListener {
    fun onServiceConnected()
    fun onServiceDisconnected()
    fun onQueueChanged()
    fun onPlayingMetaChanged()
    fun onPlayStateChanged()
    fun onRepeatModeChanged()
    fun onShuffleModeChanged()
    fun onMediaStoreChanged()
}
