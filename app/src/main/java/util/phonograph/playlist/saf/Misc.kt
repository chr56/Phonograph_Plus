/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.saf

import player.phonograph.util.reportError
import player.phonograph.util.warning
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import java.io.FileNotFoundException
import java.io.OutputStream

@SuppressLint("Recycle")
internal fun openOutputStreamSafe(context: Context, uri: Uri, mode: String): OutputStream? =
    try {
        val outputStream = context.contentResolver.openOutputStream(uri, mode)
        if (outputStream == null) warning(TAG, "Failed!")
        outputStream
    } catch (e: FileNotFoundException) {
        reportError(e, TAG, "Not found $uri")
        null
    }

private const val TAG = "Playlist"

internal const val PLAYLIST_MIME_TYPE = "audio/x-mpegurl"