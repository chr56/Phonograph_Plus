/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.setting

import player.phonograph.App
import player.phonograph.model.notification.NotificationActionsConfig
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.util.Log
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object NotificationConfig {

    var actions: NotificationActionsConfig
        get() {
            val rawString = Setting(App.Companion.instance)[Keys.notificationActionsJsonString].data
            return readFromJson(rawString, ::resetActions)
        }
        set(value) {
            val jsonString = writeToJson(value)
            Setting(App.Companion.instance)[Keys.notificationActionsJsonString].data = jsonString
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