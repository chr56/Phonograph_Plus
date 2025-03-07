/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.setting

import player.phonograph.App
import player.phonograph.model.pages.PagesConfig
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.reportError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

object HomeTabConfig {
    private val parser = Json { ignoreUnknownKeys = true; isLenient = true }

    private var cachedPagesConfig: PagesConfig? = null
    private var cachedRawPageConfig: String? = null

    var homeTabConfig: PagesConfig
        @Synchronized get() {
            val rawString = Setting(App.instance)[Keys.homeTabConfigJsonString].data

            // Fetch Cache
            val cached: PagesConfig? = cachedPagesConfig
            if (rawString == cachedRawPageConfig && cached != null) {
                return cached
            }

            // Then Parse
            val config: PagesConfig = parseHomeTabConfig(rawString)
            cachedPagesConfig = config
            cachedRawPageConfig = rawString

            return config
        }
        set(value) {
            val json = try {
                value.toJson()
            } catch (e: Exception) {
                reportError(e, "Preference", "Failed to save home tab config, using default config")
                PagesConfig.DEFAULT_CONFIG.toJson()
            }

            val str = Json.encodeToString(json)
            synchronized(this) {
                Setting(App.instance)[Keys.homeTabConfigJsonString].data = str
            }
        }

    fun parseHomeTabConfig(raw: String): PagesConfig =
        fromJson(raw) ?: run {
            resetHomeTabConfig()
        }

    /**
     * @return default config
     */
    fun resetHomeTabConfig(): PagesConfig = PagesConfig.DEFAULT_CONFIG.also { homeTabConfig = it }

    private fun fromJson(raw: String): PagesConfig? {
        if (raw.isEmpty()) return null

        return try {
            val jsonElement: JsonElement = parser.parseToJsonElement(raw)
            fromJsonElement(jsonElement)
        } catch (e: Exception) {
            reportError(e, "Preference", "Failed to parse home tab config string: $raw")
            null
        }
    }

    private fun fromJsonElement(json: JsonElement): PagesConfig? {
        val array = json.jsonObject[KEY] as? JsonArray ?: return null

        if (array.isEmpty()) return null

        val data = array.mapNotNull { (it as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() } }

        return PagesConfig(data)
    }


    private fun PagesConfig.toJson(): JsonObject =
        JsonObject(mapOf(KEY to JsonArray(tabs.map { JsonPrimitive(it) })))

    private const val KEY = "PageCfg"
}