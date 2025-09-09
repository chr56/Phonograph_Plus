/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.notification

import player.phonograph.R
import player.phonograph.model.service.ACTION_EXIT_OR_STOP
import player.phonograph.model.service.ACTION_FAST_FORWARD
import player.phonograph.model.service.ACTION_FAST_REWIND
import player.phonograph.model.service.ACTION_FAV
import player.phonograph.model.service.ACTION_NEXT
import player.phonograph.model.service.ACTION_PREVIOUS
import player.phonograph.model.service.ACTION_REPEAT
import player.phonograph.model.service.ACTION_SHUFFLE
import player.phonograph.model.service.ACTION_TOGGLE_PAUSE
import player.phonograph.model.service.MusicServiceStatus
import player.phonograph.model.service.RepeatMode
import player.phonograph.model.service.ShuffleMode
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

sealed class NotificationAction(
    @param:NotificationActionName val key: String,
    @param:StringRes val stringRes: Int,
    val action: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (other !is NotificationAction) return false
        return key == other.key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    @DrawableRes
    abstract fun icon(status: MusicServiceStatus): Int

    object PlayPause : NotificationAction(
        ACTION_KEY_PLAY_PAUSE,
        R.string.action_play_pause,
        ACTION_TOGGLE_PAUSE
    ) {
        override fun icon(status: MusicServiceStatus): Int =
            if (status.isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp
    }

    object Prev : NotificationAction(ACTION_KEY_PREV, R.string.action_previous, ACTION_PREVIOUS) {
        override fun icon(status: MusicServiceStatus): Int = R.drawable.ic_skip_previous_white_24dp
    }

    object Next : NotificationAction(ACTION_KEY_NEXT, R.string.action_next, ACTION_NEXT) {
        override fun icon(status: MusicServiceStatus): Int = R.drawable.ic_skip_next_white_24dp
    }

    object Repeat : NotificationAction(ACTION_KEY_REPEAT, R.string.action_repeat_mode, ACTION_REPEAT) {
        override fun icon(status: MusicServiceStatus): Int = when (status.repeatMode) {
            RepeatMode.NONE               -> R.drawable.ic_repeat_off_white_24dp
            RepeatMode.REPEAT_QUEUE       -> R.drawable.ic_repeat_white_24dp
            RepeatMode.REPEAT_SINGLE_SONG -> R.drawable.ic_repeat_one_white_24dp
        }
    }

    object Shuffle : NotificationAction(ACTION_KEY_SHUFFLE, R.string.action_shuffle_mode, ACTION_SHUFFLE) {
        override fun icon(status: MusicServiceStatus): Int = when (status.shuffleMode) {
            ShuffleMode.NONE    -> R.drawable.ic_shuffle_disabled_white_24dp
            ShuffleMode.SHUFFLE -> R.drawable.ic_shuffle_white_24dp
        }
    }

    object FastRewind : NotificationAction(
        ACTION_KEY_FAST_REWIND,
        R.string.action_fast_rewind,
        ACTION_FAST_REWIND
    ) {
        override fun icon(status: MusicServiceStatus): Int = R.drawable.ic_fast_rewind_white_24dp
    }

    object FastForward : NotificationAction(
        ACTION_KEY_FAST_FORWARD,
        R.string.action_fast_forward,
        ACTION_FAST_FORWARD
    ) {
        override fun icon(status: MusicServiceStatus): Int = R.drawable.ic_fast_forward_white_24dp
    }

    object Fav : NotificationAction(ACTION_KEY_FAV, R.string.playlist_favorites, ACTION_FAV) {
        override fun icon(status: MusicServiceStatus): Int = R.drawable.ic_favorite_border_white_24dp
    }

    object Close : NotificationAction(ACTION_KEY_CLOSE, R.string.action_exit, ACTION_EXIT_OR_STOP) {
        override fun icon(status: MusicServiceStatus): Int = R.drawable.ic_close_white_24dp
    }

    private object Invalid : NotificationAction(ACTION_KEY_UNKNOWN, R.string.msg_unknown, ".") {
        override fun icon(status: MusicServiceStatus): Int = R.drawable.ic_notification
    }

    companion object {
        @JvmStatic
        fun from(@NotificationActionName name: String): NotificationAction = when (name) {
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

        @JvmStatic
        val ALL_ACTIONS: List<NotificationAction> = listOf(
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

        @JvmStatic
        val COMMON_ACTIONS: List<NotificationAction> = listOf(PlayPause, Prev, Next)
    }
}


