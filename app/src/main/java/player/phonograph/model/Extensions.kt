/*
 * Copyright (c) 2022 chr_56
 */
@file:JvmName("MusicUtil")

package player.phonograph.model

import android.content.Context
import java.util.*
import player.phonograph.R

fun Song.infoString(): String =
    buildInfoString(artistName, albumName)

fun songCountString(context: Context, songCount: Int): String =
    context.resources.getQuantityString(R.plurals.x_songs, songCount, songCount)

fun Artist.infoString(context: Context): String =
    buildInfoString(
        albumCountString(context, albumCount),
        songCountString(context, songCount)
    )

fun isArtistNameUnknown(artistName: String?): Boolean = when {
    artistName.isNullOrBlank() -> false // not certain
    artistName == Artist.UNKNOWN_ARTIST_DISPLAY_NAME -> true
    artistName.trim().lowercase() == "unknown" -> true
    artistName.trim().lowercase() == "<unknown>" -> true
    else -> false
}

fun albumCountString(context: Context, albumCount: Int): String {
    val albumString = if (albumCount == 1) context.resources.getString(R.string.album) else context.resources.getString(
        R.string.albums
    )
    return "$albumCount $albumString"
}

fun Album.infoString(context: Context): String =
    buildInfoString(
        artistName,
        songCountString(context, songCount)
    )

fun Genre.infoString(context: Context): String =
    songCountString(context, songCount)

/**
 * Build a concatenated string from the provided arguments
 * The intended purpose is to show extra annotations
 * to a music library item.
 * Ex: for a given album --> buildInfoString(album.artist, album.songCount)
 */
fun buildInfoString(string1: String?, string2: String?): String =
    when {
        string1.isNullOrEmpty() && !string2.isNullOrEmpty() -> string2
        !string1.isNullOrEmpty() && string2.isNullOrEmpty() -> string1
        !string1.isNullOrEmpty() && !string2.isNullOrEmpty() -> "$string1  •  $string2"
        else -> ""
    }

fun getReadableDurationString(songDurationMillis: Long): String {
    var minutes = songDurationMillis / 1000 / 60
    val seconds = songDurationMillis / 1000 % 60
    return if (minutes < 60) {
        String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds)
    } else {
        val hours = minutes / 60
        minutes %= 60
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    }
}

fun List<Song>.totalDuration(): Long =
    fold(0L) { acc: Long, song: Song -> acc + song.duration }

fun getYearString(year: Int): String = if (year > 0) year.toString() else "-"

fun getFileSizeString(sizeInBytes: Long): String {
    val fileSizeInKB: Long = sizeInBytes / 1024
    val fileSizeInMB: Long = fileSizeInKB / 1024
    val fileSizeInMBf: Float = fileSizeInKB / 1024F

    val readableFileSizeInMB =
        fileSizeInMB.toString() +
                ((fileSizeInMBf - fileSizeInMB).toString()).let {
                    if (it.isNotBlank() && it.length >= 5) it.substring(1, 4) else ".0"
                }

    return "$readableFileSizeInMB MB ($fileSizeInKB KB)"
}