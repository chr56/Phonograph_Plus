package player.phonograph.service.util

import player.phonograph.helper.StopWatch
import player.phonograph.model.Song

/**
 * @author Abou Zeid (kabouzeid)
 */
class SongPlayCountHelper {
    private val stopWatch = StopWatch()
    var song = Song.EMPTY_SONG
        private set

    fun shouldBumpPlayCount(): Boolean = song.duration * 0.5 < stopWatch.elapsedTime

    fun notifySongChanged(song: Song) {
        synchronized(this) {
            stopWatch.reset()
            this.song = song
        }
    }

    fun notifyPlayStateChanged(isPlaying: Boolean) {
        synchronized(this) {
            if (isPlaying) {
                stopWatch.start()
            } else {
                stopWatch.pause()
            }
        }
    }
}