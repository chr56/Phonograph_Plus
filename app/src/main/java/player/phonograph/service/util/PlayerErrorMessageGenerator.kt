/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.util

import player.phonograph.R
import android.content.res.Resources
import android.media.MediaPlayer.MEDIA_ERROR_IO
import android.media.MediaPlayer.MEDIA_ERROR_MALFORMED
import android.media.MediaPlayer.MEDIA_ERROR_SERVER_DIED
import android.media.MediaPlayer.MEDIA_ERROR_TIMED_OUT
import android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN
import android.media.MediaPlayer.MEDIA_ERROR_UNSUPPORTED
import android.util.Log

internal fun makeErrorMessage(resources: Resources, what: Int, extra: Int, path: String): String {
    val generic = when (what) {
        MEDIA_ERROR_UNKNOWN     -> "Unknown"
        MEDIA_ERROR_SERVER_DIED -> "Media server unavailable, restart player!"
        else                    -> what.toString()
    }
    val detail = when (extra) {
        MEDIA_ERROR_IO          -> "(File unavailable)"
        MEDIA_ERROR_MALFORMED   -> "(Glitch of stream format)"
        MEDIA_ERROR_UNSUPPORTED -> "(Unsupported format)"
        MEDIA_ERROR_TIMED_OUT   -> "(Time out)"
        else                    -> extra.toString()
    }
    val msg = "${makeErrorMessage(resources, path)} ($generic - $detail)"
    Log.w("MediaPlayer", msg)
    return msg
}

internal fun makeErrorMessage(resources: Resources, path: String, exist: Boolean) =
    "${makeErrorMessage(resources, path)} (${if (!exist) resources.getString(R.string.deleted) else resources.getString(R.string.permissions_denied)})"

private fun makeErrorMessage(resources: Resources, path: String): String =
    "${resources.getString(R.string.unplayable_file)} ${path.removePrefix("/storage")}"
