/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.service.util


import org.koin.android.ext.android.get
import player.phonograph.ACTUAL_PACKAGE_NAME
import player.phonograph.model.Song
import player.phonograph.service.MusicService
import player.phonograph.service.queue.QueueManager
import android.content.Intent

object MusicServiceUtil {
    private const val ANDROID_MUSIC_PACKAGE_NAME = "com.android.music"
    fun sendPublicIntent(service: MusicService, what: String) {
        service.sendStickyBroadcast(
            Intent(
                what.replace(ACTUAL_PACKAGE_NAME, ANDROID_MUSIC_PACKAGE_NAME)
            ).apply {
                val queueManager: QueueManager = service.get()
                val song: Song = queueManager.currentSong
                putExtra("id", song.id)
                putExtra("artist", song.artistName)
                putExtra("album", song.albumName)
                putExtra("track", song.title)
                putExtra("duration", song.duration)
                putExtra("position", service.songProgressMillis.toLong())
                putExtra("playing", service.isPlaying)
                putExtra("scrobbling_source", ACTUAL_PACKAGE_NAME)
            }
        )
    }
}
