/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.notification

import androidx.annotation.StringDef

@StringDef(
    ACTION_KEY_PLAY_PAUSE,
    ACTION_KEY_PREV,
    ACTION_KEY_NEXT,
    ACTION_KEY_REPEAT,
    ACTION_KEY_SHUFFLE,
    ACTION_KEY_FAST_REWIND,
    ACTION_KEY_FAST_FORWARD,
    ACTION_KEY_FAV,
    ACTION_KEY_CLOSE,
    ACTION_KEY_UNKNOWN,
)
@Retention(AnnotationRetention.SOURCE)
annotation class NotificationActionName

const val ACTION_KEY_PLAY_PAUSE = "PLAY_PAUSE"
const val ACTION_KEY_PREV = "PREV"
const val ACTION_KEY_NEXT = "NEXT"
const val ACTION_KEY_REPEAT = "REPEAT"
const val ACTION_KEY_SHUFFLE = "SHUFFLE"
const val ACTION_KEY_FAST_REWIND = "FAST_REWIND"
const val ACTION_KEY_FAST_FORWARD = "FAST_FORWARD"
const val ACTION_KEY_FAV = "FAV"
const val ACTION_KEY_CLOSE = "CLOSE"
const val ACTION_KEY_UNKNOWN = "UNKNOWN"