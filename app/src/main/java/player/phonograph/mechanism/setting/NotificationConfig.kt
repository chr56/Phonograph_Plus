/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.setting

import player.phonograph.App
import player.phonograph.R
import player.phonograph.service.MusicService
import player.phonograph.service.ServiceStatus
import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringDef
import androidx.annotation.StringRes
import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object NotificationConfig {

    var actions: NotificationActionsConfig
        get() {
            val rawString = Setting(App.instance)[Keys.notificationActionsJsonString].data
            return readFromJson(rawString, ::resetActions)
        }
        set(value) {
            val jsonString = writeToJson(value)
            Setting(App.instance)[Keys.notificationActionsJsonString].data = jsonString
        }

    fun resetActions(): NotificationActionsConfig {
        return NotificationActionsConfig.DEFAULT.also { default ->
            actions = default
        }
    }

    private fun readFromJson(
        raw: String,
        resetToDefault: () -> NotificationActionsConfig,
    ): NotificationActionsConfig = try {
        parser.decodeFromString<NotificationActionsConfig>(raw)
            .takeIf { it.actions.isNotEmpty() } ?: resetToDefault()
    } catch (e: SerializationException) {
        Log.e(TAG, "Glitch Config: $raw", e)
        resetToDefault()
    }

    private fun writeToJson(
        config: NotificationActionsConfig,
    ): String {
        return try {
            parser.encodeToString(config)
        } catch (e: SerializationException) {
            Log.e(TAG, "Failed to serialize", e)
            return "{}"
        }
    }

    private val parser
        get() = Json {
            ignoreUnknownKeys = true
        }

    private const val TAG = "NotificationConfig"
}

@Keep
@Parcelize
@Serializable
data class NotificationActionsConfig(
    @SerialName("actions") val actions: List<Item>,
    @SerialName("version") val version: Int = VERSION,
) : Parcelable {

    constructor(vararg sources: Item) : this(sources.asList(), VERSION)

    @Keep
    @Parcelize
    @Serializable
    data class Item(
        @SerialName("key") @NotificationActionName val key: String,
        @SerialName("compat") var displayInCompat: Boolean = false,
    ) : Parcelable {

        @Contextual
        @IgnoredOnParcel
        val notificationAction: NotificationAction = NotificationAction.from(key)
    }

    companion object {
        val DEFAULT: NotificationActionsConfig
            get() = NotificationActionsConfig(
                Item(ACTION_KEY_REPEAT),
                Item(ACTION_KEY_PREV, displayInCompat = true),
                Item(ACTION_KEY_PLAY_PAUSE, displayInCompat = true),
                Item(ACTION_KEY_NEXT, displayInCompat = true),
                Item(ACTION_KEY_SHUFFLE),
            )
        const val VERSION = 1
    }
}

@Suppress("ConvertObjectToDataObject")
sealed class NotificationAction(
    @NotificationActionName val key: String,
    @get:StringRes val stringRes: Int,
    val action: String,
) {

    @DrawableRes
    open fun icon(status: ServiceStatus): Int = 0

    object PlayPause : NotificationAction(
        ACTION_KEY_PLAY_PAUSE,
        R.string.action_play_pause,
        MusicService.ACTION_TOGGLE_PAUSE
    ) {
        override fun icon(status: ServiceStatus): Int =
            if (status.isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_arrow_white_24dp
    }

    object Prev : NotificationAction(ACTION_KEY_PREV, R.string.action_previous, MusicService.ACTION_REWIND) {
        override fun icon(status: ServiceStatus): Int = R.drawable.ic_skip_previous_white_24dp
    }

    object Next : NotificationAction(ACTION_KEY_NEXT, R.string.action_next, MusicService.ACTION_SKIP) {
        override fun icon(status: ServiceStatus): Int = R.drawable.ic_skip_next_white_24dp
    }

    object Repeat : NotificationAction(ACTION_KEY_REPEAT, R.string.action_repeat_mode, MusicService.ACTION_REPEAT) {
        override fun icon(status: ServiceStatus): Int = when (status.repeatMode) {
            RepeatMode.NONE               -> R.drawable.ic_repeat_off_white_24dp
            RepeatMode.REPEAT_QUEUE       -> R.drawable.ic_repeat_white_24dp
            RepeatMode.REPEAT_SINGLE_SONG -> R.drawable.ic_repeat_one_white_24dp
        }
    }

    object Shuffle : NotificationAction(ACTION_KEY_SHUFFLE, R.string.action_shuffle_mode, MusicService.ACTION_SHUFFLE) {
        override fun icon(status: ServiceStatus): Int = when (status.shuffleMode) {
            ShuffleMode.NONE    -> R.drawable.ic_shuffle_disabled_white_24dp
            ShuffleMode.SHUFFLE -> R.drawable.ic_shuffle_white_24dp
        }
    }

    object Fav : NotificationAction(ACTION_KEY_FAV, R.string.favorites, MusicService.ACTION_FAV) {
        override fun icon(status: ServiceStatus): Int = R.drawable.ic_favorite_border_white_24dp
    }

    object Close : NotificationAction(ACTION_KEY_CLOSE, R.string.exit, MusicService.ACTION_EXIT_OR_STOP) {
        override fun icon(status: ServiceStatus): Int = R.drawable.ic_close_white_24dp
    }

    private object Invalid : NotificationAction(ACTION_KEY_UNKNOWN, R.string.empty, ".") {
        override fun icon(status: ServiceStatus): Int = R.drawable.ic_notification
    }

    companion object {
        @JvmStatic
        fun from(@NotificationActionName name: String): NotificationAction = when (name) {
            ACTION_KEY_PLAY_PAUSE -> PlayPause
            ACTION_KEY_PREV       -> Prev
            ACTION_KEY_NEXT       -> Next
            ACTION_KEY_REPEAT     -> Repeat
            ACTION_KEY_SHUFFLE    -> Shuffle
            ACTION_KEY_FAV        -> Fav
            ACTION_KEY_CLOSE      -> Close
            else                  -> Invalid
        }

        @JvmStatic
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
    ACTION_KEY_UNKNOWN,
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
private const val ACTION_KEY_UNKNOWN = "UNKNOWN"