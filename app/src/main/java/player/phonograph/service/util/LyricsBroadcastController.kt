/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.service.util

import player.phonograph.foundation.permission.StoragePermissionChecker
import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.mechanism.lyrics.LyricsLoader
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.service.MusicService
import player.phonograph.service.ServiceComponent
import android.content.Context
import android.util.Log
import kotlin.math.max
import java.io.File

class LyricsBroadcastController {

    private var lyrics: LrcLyrics? = null

    private var current: String = ""

    fun broadcast(processInMills: Int) {
        val newLine = lyrics?.getLine(max(processInMills - 100, 0))?.first
        if (newLine != null) {
            if (newLine != current) {
                current = newLine // update cache
                StatusBarLyric.updateLyric(newLine)
            }
        } else {
            current = ""
            stop()
        }
    }

    fun load(new: LrcLyrics) {
        lyrics = new
    }

    suspend fun load(context: Context, song: Song) {
        try {
            lyrics = search(context, song)
        } catch (e: Exception) {
            Log.e("Lyrics", "Failed to update lyrics!", e)
        }
    }

    private suspend fun search(context: Context, song: Song): LrcLyrics? {
        val file = File(song.data)
        return if (StoragePermissionChecker.hasStorageReadPermission(context) && file.exists()) {
            LyricsLoader.search(file, song.title).firstLrcLyrics()
        } else {
            null
        }
    }

    fun clear() {
        current = ""
        lyrics = null
    }

    fun stop() {
        StatusBarLyric.stopLyric()
    }
}
