/*
 *  Copyright (c) 2022~2025 chr_56
 */

@file:JvmName("MusicTextUtil")

package player.phonograph.util.text

import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import android.content.Context

/**
 * Build a concatenated string from the provided arguments
 * The intended purpose is to show extra annotations
 * to a music library item.
 * Ex: for a given album --> buildInfoString(album.artist, album.songCount)
 */
fun buildInfoString(string1: String?, string2: String?): String =
    when {
        string1.isNullOrEmpty() && !string2.isNullOrEmpty()  -> string2
        !string1.isNullOrEmpty() && string2.isNullOrEmpty()  -> string1
        !string1.isNullOrEmpty() && !string2.isNullOrEmpty() -> "$string1  •  $string2"
        else                                                 -> ""
    }

fun Song.infoString(): String =
    buildInfoString(
        artistName,
        albumName
    )

fun Artist.infoString(context: Context): String =
    buildInfoString(
        albumCountString(context, albumCount),
        songCountString(context, songCount)
    )

fun Album.infoString(context: Context): String =
    buildInfoString(
        artistName,
        songCountString(context, songCount)
    )

fun Genre.infoString(context: Context): String =
    songCountString(
        context,
        songCount
    )


fun songCountString(context: Context, songCount: Int): String =
    "$songCount ${if (songCount == 1) context.resources.getString(R.string.song) else context.resources.getString(R.string.songs)}"

fun albumCountString(context: Context, albumCount: Int): String =
    "$albumCount ${if (albumCount == 1) context.resources.getString(R.string.album) else context.resources.getString(R.string.albums)}"

fun readableYear(year: Int): String = if (year > 0) year.toString() else "-"