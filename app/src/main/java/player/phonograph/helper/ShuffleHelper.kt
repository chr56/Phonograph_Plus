package player.phonograph.helper

import player.phonograph.model.Song
import java.util.*

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object ShuffleHelper {
    @JvmStatic
    fun makeShuffleList(listToShuffle: MutableList<Song>, current: Int) {
        if (listToShuffle.isEmpty()) return

        if (current >= 0) {
            val song: Song = listToShuffle.removeAt(current)
            listToShuffle.shuffle()
            listToShuffle.add(0, song)
        } else {
            listToShuffle.shuffle()
        }

    }
}