package player.phonograph.model

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
interface MusicServiceEventListener {
    fun onServiceConnected()
    fun onServiceDisconnected()
    fun onPlayingMetaChanged()
}
