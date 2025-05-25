/*
 *  Copyright (c) 2022~2023 chr_56
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 3,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 */

package player.phonograph.util

import player.phonograph.BuildConfig.DEBUG
import player.phonograph.foundation.error.warning
import player.phonograph.model.Song
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream


//
// debug
//
/**
 * only run [block] on [DEBUG] build
 */
inline fun debug(crossinline block: () -> Unit) {
    if (DEBUG) block()
}

//
// Bit Mask
//

fun Int.testBit(mask: Int): Boolean = (this and mask) != 0
fun Int.setBit(mask: Int): Int = (this or mask)
fun Int.unsetBit(mask: Int): Int = (this and mask.inv())


//
// Context check
//

inline fun activity(context: Context, block: (Activity) -> Boolean): Boolean =
    if (context is Activity) {
        block(context)
    } else {
        false
    }

inline fun fragmentActivity(context: Context, block: (FragmentActivity) -> Boolean): Boolean =
    if (context is FragmentActivity) {
        block(context)
    } else {
        false
    }


//
// Reflection
//

@Throws(NoSuchFieldException::class, SecurityException::class)
inline fun <reified T, reified F> T.reflectDeclaredField(fieldName: String): F {
    val f =
        T::class.java.getDeclaredField(fieldName).apply {
            isAccessible = true
        }
    return f.get(this) as F
}

//
// Sort
//

inline fun <T> List<T>.sort(
    revert: Boolean,
    crossinline selector: (T) -> Comparable<*>?,
): List<T> {
    return if (revert) this.sortedWith(compareByDescending(selector))
    else this.sortedWith(compareBy(selector))
}

//
// Metrics

fun logMetrics(stage: String) {
    Log.v(
        "Metrics",
        "[${System.currentTimeMillis().mod(100000)}] $stage"
    )
}

//
// Other
//


fun openOutputStreamSafe(context: Context, uri: Uri, mode: String): OutputStream? =
    try {
        @SuppressLint("Recycle")
        val outputStream = context.contentResolver.openOutputStream(uri, mode)
        if (outputStream == null) warning(context, "UriUtil", "Failed to open ${uri.path}")
        outputStream
    } catch (e: FileNotFoundException) {
        warning(context, "UriUtil", "File Not found (${uri.path})", e)
        null
    }

internal const val PLAYLIST_MIME_TYPE = "audio/x-mpegurl"

fun setRingtone(context: Context, songId: Long) {
    RingtoneManager.setActualDefaultRingtoneUri(
        context,
        RingtoneManager.TYPE_ALARM,
        mediaStoreUriSong(MEDIASTORE_VOLUME_EXTERNAL, songId)
    )
}

fun shareFileIntent(context: Context, song: Song): Intent {
    return try {
        Intent()
            .setAction(Intent.ACTION_SEND)
            .putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(context, context.applicationContext.packageName, File(song.data))
            )
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .setType("audio/*")
    } catch (e: IllegalArgumentException) {
        // the path is most likely not like /storage/emulated/0/... but something like /storage/28C7-75B0/...
        warning(context, "Share", "Physical external SD card is not fully support!", e)
        Intent()
    }
}

fun Song?.asList(): List<Song> = if (this != null) listOf(this) else emptyList()

