/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.notification

import player.phonograph.model.service.ACTION_EXIT_OR_STOP
import player.phonograph.model.service.ACTION_FAST_FORWARD
import player.phonograph.model.service.ACTION_FAST_REWIND
import player.phonograph.model.service.ACTION_NEXT
import player.phonograph.model.service.ACTION_PAUSE
import player.phonograph.model.service.ACTION_PLAY
import player.phonograph.model.service.ACTION_PREVIOUS
import player.phonograph.model.service.ACTION_REPEAT
import player.phonograph.model.service.ACTION_SHUFFLE
import player.phonograph.model.service.ACTION_TOGGLE_PAUSE
import androidx.annotation.StringDef

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

enum class NotificationAction(
    @get:NotificationActionName @field:NotificationActionName val key: String,
    val command: String,
) {
    PlayPause(ACTION_KEY_PLAY_PAUSE, ACTION_TOGGLE_PAUSE),
    Prev(ACTION_KEY_PREV, ACTION_PREVIOUS),
    Next(ACTION_KEY_NEXT, ACTION_NEXT),
    Repeat(ACTION_KEY_REPEAT, ACTION_REPEAT),
    Shuffle(ACTION_KEY_SHUFFLE, ACTION_SHUFFLE),
    FastForward(ACTION_KEY_FAST_FORWARD, ACTION_FAST_FORWARD),
    FastRewind(ACTION_KEY_FAST_REWIND, ACTION_FAST_REWIND),
    Fav(ACTION_KEY_FAV, ACTION_KEY_FAV),
    Close(ACTION_KEY_CLOSE, ACTION_EXIT_OR_STOP),
    Invalid(ACTION_KEY_UNKNOWN, "");

    companion object {
        @JvmStatic
        fun from(@NotificationActionName name: String) = when (name) {
            ACTION_KEY_PLAY_PAUSE   -> PlayPause
            ACTION_KEY_PREV         -> Prev
            ACTION_KEY_NEXT         -> Next
            ACTION_KEY_REPEAT       -> Repeat
            ACTION_KEY_SHUFFLE      -> Shuffle
            ACTION_KEY_FAST_REWIND  -> FastRewind
            ACTION_KEY_FAST_FORWARD -> FastForward
            ACTION_KEY_FAV          -> Fav
            ACTION_KEY_CLOSE        -> Close
            else                    -> Invalid
        }

        val ALL: List<NotificationAction>
            get() = listOf(
                PlayPause,
                Prev,
                Next,
                Repeat,
                Shuffle,
                FastForward,
                FastRewind,
                Fav,
                Close,
            )


        val COMMON: List<NotificationAction>
            get() = listOf(
                PlayPause,
                Prev,
                Next,
            )
    }
}