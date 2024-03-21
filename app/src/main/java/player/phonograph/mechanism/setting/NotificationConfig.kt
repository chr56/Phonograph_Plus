/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.setting

import player.phonograph.R
import androidx.annotation.StringDef
import androidx.annotation.StringRes

object NotificationActionsConfig {

    var compat: List<NotificationAction>
        get() = emptyList()
        set(value) {}

    var expanded: List<NotificationAction>
        get() = emptyList()
        set(value) {}

    fun resetCompat(): List<NotificationAction> = DEFAULT_CONFIG_COMPAT
    fun resetExpanded(): List<NotificationAction> = DEFAULT_CONFIG_EXPANDED

    private fun readImpl(
        raw: String,
        resetToDefault: () -> List<NotificationAction>,
    ): List<NotificationAction> {
        val actionList = raw.split(SEPARATOR).mapNotNull { NotificationAction.from(it) }
        return actionList.ifEmpty { resetToDefault() }
    }

    private fun writeImpl(
        action: List<NotificationAction>,
    ): String {
        return action.joinToString(separator = SEPARATOR.toString())
    }

    val DEFAULT_CONFIG_EXPANDED: List<NotificationAction>
        get() = listOf(
            NotificationAction.Repeat,
            NotificationAction.Prev,
            NotificationAction.PlayPause,
            NotificationAction.Next,
            NotificationAction.Shuffle,
        )


    val DEFAULT_CONFIG_COMPAT: List<NotificationAction>
        get() = listOf(
            NotificationAction.Prev,
            NotificationAction.PlayPause,
            NotificationAction.Next,
        )

    private const val SEPARATOR = ','
}


@Suppress("ConvertObjectToDataObject")
sealed class NotificationAction(
    @NotificationActionName private val key: String,
    @get:StringRes val stringRes: Int,
) {
    object PlayPause : NotificationAction(ACTION_KEY_PLAY_PAUSE, R.string.action_play_pause)
    object Prev : NotificationAction(ACTION_KEY_PREV, R.string.action_previous)
    object Next : NotificationAction(ACTION_KEY_NEXT, R.string.action_next)
    object Repeat : NotificationAction(ACTION_KEY_REPEAT, R.string.action_repeat_mode)
    object Shuffle : NotificationAction(ACTION_KEY_SHUFFLE, R.string.action_shuffle_mode)
    object Fav : NotificationAction(ACTION_KEY_FAV, R.string.favorites)
    object Close : NotificationAction(ACTION_KEY_CLOSE, R.string.exit)

    companion object {
        @JvmStatic
        fun from(@NotificationActionName name: String): NotificationAction? = when (name) {
            ACTION_KEY_PLAY_PAUSE -> PlayPause
            ACTION_KEY_PREV       -> Prev
            ACTION_KEY_NEXT       -> Next
            ACTION_KEY_REPEAT     -> Repeat
            ACTION_KEY_SHUFFLE    -> Shuffle
            ACTION_KEY_FAV        -> Fav
            ACTION_KEY_CLOSE      -> Close
            else                  -> null
        }

        val ALL_ACTIONS: List<NotificationAction> = listOf(
            PlayPause,
            Prev,
            Next,
            Repeat,
            Shuffle,
            Fav,
            Close,
        )
    }
}

@StringDef(
    ACTION_KEY_PLAY_PAUSE,
    ACTION_KEY_PREV,
    ACTION_KEY_NEXT,
    ACTION_KEY_REPEAT,
    ACTION_KEY_SHUFFLE,
    ACTION_KEY_FAV,
    ACTION_KEY_CLOSE,
)
@Retention(AnnotationRetention.SOURCE)
annotation class NotificationActionName

private const val ACTION_KEY_PLAY_PAUSE = "PLAY_PAUSE"
private const val ACTION_KEY_PREV = "PREV"
private const val ACTION_KEY_NEXT = "NEXT"
private const val ACTION_KEY_REPEAT = "REPEAT"
private const val ACTION_KEY_SHUFFLE = "SHUFFLE"
private const val ACTION_KEY_FAV = "FAV"
private const val ACTION_KEY_CLOSE = "CLOSE"