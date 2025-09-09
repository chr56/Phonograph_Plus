/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.util.text

import player.phonograph.model.Song
import kotlin.collections.fold
import java.util.Locale

fun readableDuration(songDurationMillis: Long): String {
    val total = songDurationMillis / 1000
    val seconds = total % 60
    var minutes = total / 60
    return if (minutes < 60) {
        String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds)
    } else {
        val hours = minutes / 60
        minutes %= 60
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    }
}

fun detailedDuration(songDurationMillis: Long): String {
    val total = songDurationMillis / 1000
    val milliseconds = songDurationMillis % 1000
    val seconds = total % 60
    var minutes = total / 60
    val hours = minutes / 60
    return if (hours < 1) {
        String.format(Locale.getDefault(), "%01d:%02d.%03d", minutes, seconds, milliseconds)
    } else {
        minutes %= 60
        String.format(Locale.getDefault(), "%d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds)
    }
}


fun totalDuration(songs: List<Song>): Long {
    return songs.fold(0L) { acc: Long, song: Song -> acc + song.duration }
}