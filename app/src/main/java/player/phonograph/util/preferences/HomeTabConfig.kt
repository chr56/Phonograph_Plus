/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.preferences

import player.phonograph.model.pages.PageConfig
import player.phonograph.model.pages.PageConfigUtil
import player.phonograph.model.pages.PageConfigUtil.toJson
import player.phonograph.settings.Setting
import player.phonograph.util.Util
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

object HomeTabConfig {
    private val parser = Json { ignoreUnknownKeys = true;isLenient = true }

    private var cachedPageConfig: PageConfig? = null
    private var cachedRawPageConfig: String? = null

    var homeTabConfig: PageConfig
        @Synchronized get() {
            val rawString = Setting.instance.homeTabConfigJsonString

            if (rawString.isEmpty()) {
                resetHomeTabConfig()
                return PageConfig.DEFAULT_CONFIG
            }


            val cached = cachedPageConfig
            if (rawString == cachedRawPageConfig && cached != null) {
                return cached // return the cached instead
            }


            val config: PageConfig = try {
                val json = parser.parseToJsonElement(rawString)
                PageConfigUtil.fromJson(json as JsonObject)
            } catch (e: Exception) {
                Util.reportError(e, "Preference", "Fail to parse home tab config string $rawString")
                // return default
                resetHomeTabConfig()
                PageConfig.DEFAULT_CONFIG
            }


            // update cache
            cachedPageConfig = config
            cachedRawPageConfig = rawString


            // valid // TODO
            return config
        }
        set(value) {
            val json =
                try {
                    value.toJson()
                } catch (e: Exception) {
                    Log.e("Preference", "Save home tab config failed, use default. \n${e.message}")
                    // return default
                    PageConfig.DEFAULT_CONFIG.toJson()
                }
            val str =
                parser.encodeToString(json)
            synchronized(this) {
                Setting.instance.homeTabConfigJsonString = str
            }
        }

    /**
     * add a new [page] at the end of setting
     */
    fun append(page: String) {
        val list = homeTabConfig.tabList.toMutableList()
        list.add(page)
        homeTabConfig = PageConfig.from(list)
    }

    fun resetHomeTabConfig() {
        Setting.instance.homeTabConfigJsonString =
            Json.encodeToString(PageConfig.DEFAULT_CONFIG.toJson())
    }
}
