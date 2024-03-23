/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.setting

import player.phonograph.App
import player.phonograph.R
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import androidx.annotation.Keep
import androidx.annotation.StringDef
import androidx.annotation.StringRes
import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize
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
        @SerialName("compat") val displayInCompat: Boolean = false,
    ) : Parcelable {
        val notificationAction: NotificationAction?
            get() = NotificationAction.from(key)
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