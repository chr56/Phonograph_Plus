/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.resource

import java.util.Locale

object Durations {
    fun short(songDurationMillis: Long): String {
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

    fun long(songDurationMillis: Long): String {
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
}