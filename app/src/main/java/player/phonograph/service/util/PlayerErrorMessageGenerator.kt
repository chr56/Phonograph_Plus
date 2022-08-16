/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.util

import android.content.Context
import android.media.MediaPlayer.*
import android.util.Log
import player.phonograph.R

internal fun makeErrorMessage(context: Context, what: Int, extra: Int): String {
    val generic = when (what) {
        MEDIA_ERROR_UNKNOWN -> "Unknown"
        MEDIA_ERROR_SERVER_DIED -> "Media server unavailable, restart player!"
        else -> null
    }
    val detail = when (extra) {
        MEDIA_ERROR_IO -> "(File unavailable)"
        MEDIA_ERROR_MALFORMED -> "(Glitch of stream format)"
        MEDIA_ERROR_UNSUPPORTED -> "(Unsupported format)"
        MEDIA_ERROR_TIMED_OUT -> "(Time out)"
        else -> null
    }
    val msg = "${context.getString(R.string.unplayable_file)}: $generic$detail"
    Log.w("MediaPlayer", msg)
    return msg
}
