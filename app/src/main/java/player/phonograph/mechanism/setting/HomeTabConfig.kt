/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.setting

import player.phonograph.App
import player.phonograph.mechanism.setting.HomeTabConfig.PageConfigUtil.toJson
import player.phonograph.model.pages.PagesConfig
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.reportError
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
            val json =
                try {
                    value.toJson()
                } catch (e: Exception) {
                    Log.e("Preference", "Save home tab config failed, use default. \n${e.message}")
                    // return default
                    PagesConfig.Companion.DEFAULT_CONFIG.toJson()
                }
            val str = parser.encodeToString(json)
            synchronized(this) {
                Setting(App.instance)[Keys.homeTabConfigJsonString].data = str
            }
        }

    fun parseHomeTabConfig(raw: String): PagesConfig {
        val config: PagesConfig? = PageConfigUtil.from(raw)
        return if (config == null) {
            resetHomeTabConfig()
            PagesConfig.Companion.DEFAULT_CONFIG
        } else {
            config
        }
    }

    /**
     * add a new [page] at the end of setting
     */
    fun append(page: String) {
        val list = homeTabConfig.tabs.toMutableList()
        list.add(page)
        homeTabConfig = PagesConfig(list)
    }

    fun resetHomeTabConfig() {
        Setting(App.instance)[Keys.homeTabConfigJsonString].data =
            Json.encodeToString(PagesConfig.Companion.DEFAULT_CONFIG.toJson())
    }

    object PageConfigUtil {

        fun PagesConfig.toJson(): JsonObject = JsonObject(
            mapOf(KEY to JsonArray(tabs.map { JsonPrimitive(it) }))
        )

        /**
         * Parse from raw json
         * @return null if failed
         */
        fun from(raw: String): PagesConfig? {
            if (raw.isEmpty()) return null

            val config: PagesConfig? = try {
                val json: JsonElement = parser.parseToJsonElement(raw)
                fromJson(json as JsonObject)
            } catch (e: Exception) {
                reportError(e, "Preference", "Fail to parse home tab config string $raw")
                null
            }

            // valid
            // TODO

            return config
        }

        /**
         * Parse from [JsonObject]
         */
        @Throws(IllegalStateException::class)
        fun fromJson(json: JsonObject): PagesConfig {
            val array = (json[KEY] as? JsonArray)
                ?: throw IllegalStateException("KEY(\"PageCfg\") doesn't exist")

            if (array.isEmpty()) throw IllegalStateException("No Value")

            val data = array.mapNotNull { (it as? JsonPrimitive)?.content }.filter { it.isNotBlank() }

            return PagesConfig(data)
        }

        private const val KEY = "PageCfg"

    }
}
