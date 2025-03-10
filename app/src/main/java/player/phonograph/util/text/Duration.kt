/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.util.text

import player.phonograph.model.Song
import kotlin.collections.fold
import java.util.Locale

fun readableDuration(songDurationMillis: Long): String {
    var minutes = songDurationMillis / 1000 / 60
    val seconds = songDurationMillis / 1000 % 60
    return if (minutes < 60) {
        String.Companion.format(Locale.getDefault(), "%01d:%02d", minutes, seconds)
    } else {
        val hours = minutes / 60
        minutes %= 60
        String.Companion.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    }
}

fun totalDuration(songs: List<Song>): Long {
    return songs.fold(0L) { acc: Long, song: Song -> acc + song.duration }
}