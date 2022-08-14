package player.phonograph.helper

import player.phonograph.model.Song

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object ShuffleHelper {
    @JvmStatic
    fun MutableList<Song>.shuffleAt(current: Int) {
        if (isEmpty()) return

        if (current in 0 until size) {
            val song: Song = removeAt(current)
            shuffle()
            add(0, song)
        } else {
            shuffle()
        }
    }
}
