/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.preferences

import player.phonograph.model.config.ImageSourceConfig
import player.phonograph.settings.Setting
import android.util.Log
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

object CoilImageSourceConfig {

    var currentConfig: ImageSourceConfig
        get() {
            val rawString = Setting.instance.imageSourceConfigJsonString
            val config: ImageSourceConfig = try {
                readFromJson(rawString)
            } catch (e: SerializationException) {
                Log.e(TAG, "Glitch ImageSourceConfig: $rawString", e)
                resetToDefault()
                // return default
                ImageSourceConfig.DEFAULT
            }
            return if (checkInvalid(config)) {
                config
            } else {
                resetToDefault()
                ImageSourceConfig.DEFAULT
            }
        }
        set(value) {
            val json = writeToJson(value) as JsonObject
            Setting.instance.imageSourceConfigJsonString = json.toString()
        }

    fun resetToDefault() {
        currentConfig = ImageSourceConfig.DEFAULT
    }

    private fun writeToJson(config: ImageSourceConfig): JsonElement {
        val parser = Json {
            encodeDefaults = true
        }
        return parser.encodeToJsonElement(config)
    }

    private fun readFromJson(string: String): ImageSourceConfig {
        val parser = Json {
            ignoreUnknownKeys = true
        }
        return parser.decodeFromString(string)
    }

    private fun readFromJson(jsonElement: JsonElement): ImageSourceConfig {
        val parser = Json {
            ignoreUnknownKeys = true
        }
        return parser.decodeFromJsonElement(jsonElement)
    }

    /**
     * @return true if correct
     */
    fun checkInvalid(config: ImageSourceConfig): Boolean {
        if (config.list.isEmpty()) {
            Log.e(TAG, "Illegal ImageSourceConfig: $config")
            return false
        }
        return true
    }

    private const val TAG = "ImageSource"
}