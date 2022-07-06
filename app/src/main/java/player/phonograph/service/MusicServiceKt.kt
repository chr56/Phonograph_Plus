/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.service

import android.content.Intent
import android.net.Uri
import player.phonograph.App
import player.phonograph.model.Song
import player.phonograph.util.MusicUtil.getSongFileUri

object MusicServiceKt {
    private const val ANDROID_MUSIC_PACKAGE_NAME = "com.android.music"

    @JvmStatic
    fun sendPublicIntent(service: MusicService, what: String) {
        service.sendStickyBroadcast(
            Intent(what.replace(MusicService.PHONOGRAPH_PACKAGE_NAME, ANDROID_MUSIC_PACKAGE_NAME)).apply {
                val song: Song = App.instance.queueManager.currentSong
                putExtra("id", song.id)
                putExtra("artist", song.artistName)
                putExtra("album", song.albumName)
                putExtra("track", song.title)
                putExtra("duration", song.duration)
                putExtra("position", service.songProgressMillis.toLong())
                putExtra("playing", service.isPlaying)
                putExtra("scrobbling_source", MusicService.PHONOGRAPH_PACKAGE_NAME)
            }
        )
    }

    @JvmStatic
    fun getTrackUri(song: Song): Uri {
        return getSongFileUri(song.id)
    }
}
