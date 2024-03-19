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
            NotificationAction.ACTION_REPEAT,
            NotificationAction.ACTION_PREV,
            NotificationAction.ACTION_PLAY_PAUSE,
            NotificationAction.ACTION_NEXT,
            NotificationAction.ACTION_SHUFFLE,
        )


    val DEFAULT_CONFIG_COMPAT: List<NotificationAction>
        get() = listOf(
            NotificationAction.ACTION_PREV,
            NotificationAction.ACTION_PLAY_PAUSE,
            NotificationAction.ACTION_NEXT,
        )

    private const val SEPARATOR = ','
}

@JvmInline
value class NotificationAction(@NotificationActionName val name: String) {

    @get:StringRes
    val stringRes: Int
        get() = when (name) {
            ACTION_KEY_PLAY_PAUSE -> R.string.action_play_pause
            ACTION_KEY_PREV       -> R.string.action_previous
            ACTION_KEY_NEXT       -> R.string.action_next
            ACTION_KEY_REPEAT     -> R.string.action_repeat_mode
            ACTION_KEY_SHUFFLE    -> R.string.action_shuffle_mode
            ACTION_KEY_FAV        -> R.string.favorites
            ACTION_KEY_CLOSE      -> R.string.exit
            else                  -> 0
        }

    @StringDef(
        NotificationAction.ACTION_KEY_PLAY_PAUSE,
        NotificationAction.ACTION_KEY_PREV,
        NotificationAction.ACTION_KEY_NEXT,
        NotificationAction.ACTION_KEY_REPEAT,
        NotificationAction.ACTION_KEY_SHUFFLE,
        NotificationAction.ACTION_KEY_FAV,
        NotificationAction.ACTION_KEY_CLOSE,
    )
    @Retention(AnnotationRetention.BINARY)
    annotation class NotificationActionName

    companion object {
        const val ACTION_KEY_PLAY_PAUSE = "PLAY_PAUSE";
        const val ACTION_KEY_PREV = "PREV";
        const val ACTION_KEY_NEXT = "NEXT";
        const val ACTION_KEY_REPEAT = "REPEAT";
        const val ACTION_KEY_SHUFFLE = "SHUFFLE";
        const val ACTION_KEY_FAV = "FAV";
        const val ACTION_KEY_CLOSE = "CLOSE";

        val ACTION_PLAY_PAUSE: NotificationAction get() = NotificationAction(ACTION_KEY_PLAY_PAUSE)
        val ACTION_PREV: NotificationAction get() = NotificationAction(ACTION_KEY_PREV)
        val ACTION_NEXT: NotificationAction get() = NotificationAction(ACTION_KEY_NEXT)
        val ACTION_REPEAT: NotificationAction get() = NotificationAction(ACTION_KEY_REPEAT)
        val ACTION_SHUFFLE: NotificationAction get() = NotificationAction(ACTION_KEY_SHUFFLE)
        val ACTION_FAV: NotificationAction get() = NotificationAction(ACTION_KEY_FAV)
        val ACTION_CLOSE: NotificationAction get() = NotificationAction(ACTION_KEY_CLOSE)

        val ALL_ACTIONS: List<NotificationAction>
            get() = listOf(
                ACTION_PLAY_PAUSE,
                ACTION_PREV,
                ACTION_NEXT,
                ACTION_REPEAT,
                ACTION_SHUFFLE,
                ACTION_FAV,
                ACTION_CLOSE,
            )

        fun from(@NotificationActionName name: String): NotificationAction? = when (name.trim()) {
            ACTION_KEY_PLAY_PAUSE -> ACTION_PLAY_PAUSE
            ACTION_KEY_PREV       -> ACTION_PREV
            ACTION_KEY_NEXT       -> ACTION_NEXT
            ACTION_KEY_REPEAT     -> ACTION_REPEAT
            ACTION_KEY_SHUFFLE    -> ACTION_SHUFFLE
            ACTION_KEY_FAV        -> ACTION_FAV
            ACTION_KEY_CLOSE      -> ACTION_CLOSE
            else                  -> null
        }
    }

}