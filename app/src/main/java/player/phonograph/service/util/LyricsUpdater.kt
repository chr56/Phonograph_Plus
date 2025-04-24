/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.service.util

import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.mechanism.lyrics.LyricsLoader
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.service.MusicService
import player.phonograph.service.ServiceComponent
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.permissions.StoragePermissionChecker
import android.content.Context
import kotlin.math.max
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class LyricsUpdater : ServiceComponent {

    override var created: Boolean = true // stateless

    private var lyrics: LrcLyrics? = null

    suspend fun updateViaSong(context: Context, song: Song?) {
        if (song == null) return
        val enableLyrics = Setting(context)[Keys.enableLyrics].read()
        if (!enableLyrics) return
        val file = File(song.data)
        lyrics =
            if (StoragePermissionChecker.hasStorageReadPermission(context) && file.exists()) {
                LyricsLoader.search(file, song.title).firstLrcLyrics()
            } else {
                null
            }
    }

    fun updateViaLyrics(new: LrcLyrics) {
        lyrics = new
    }

    override fun onCreate(musicService: MusicService) {
        val song = musicService.queueManager.currentSong
        musicService.coroutineScope.launch(SupervisorJob()) {
            updateViaSong(musicService, song)
        }
    }

    override fun onDestroy(musicService: MusicService) {
        clear()
    }

    /**
     * cached lyrics line
     */
    private var cache: String = ""

    /**
     * broadcast lyrics
     */
    fun broadcast(processInMills: Int) {
        val newLine = lyrics?.getLine(max(processInMills - 100, 0))?.first
        if (newLine != null) {
            if (newLine != cache) {
                cache = newLine // update cache
                StatusBarLyric.updateLyric(newLine)
            }
        } else {
            cache = ""
            StatusBarLyric.stopLyric()
        }
    }

    fun clear() {
        cache = ""
        lyrics = null
    }

}
