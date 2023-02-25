/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.preferences

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import player.phonograph.model.pages.PageConfig
import player.phonograph.model.pages.PageConfigUtil
import player.phonograph.model.pages.PageConfigUtil.fromJson
import player.phonograph.model.pages.PageConfigUtil.toJson
import player.phonograph.settings.Setting
import player.phonograph.util.Util

object HomeTabConfig {
    var homeTabConfig: PageConfig
        get() {
            val rawString = Setting.instance.homeTabConfigJsonString
            val config: PageConfig = try {
                JSONObject(rawString).fromJson()
            } catch (e: JSONException) {
                Util.reportError(e, "Preference", "Fail to parse home tab config string $rawString")
                // return default
                PageConfig.DEFAULT_CONFIG
            }
            // valid // TODO
            return config
        }
        set(value) {
            val json =
                try {
                    value.toJson()
                } catch (e: JSONException) {
                    Log.e("Preference", "Save home tab config failed, use default. \n${e.message}")
                    // return default
                    PageConfigUtil.DEFAULT_CONFIG
                }
            Setting.instance.homeTabConfigJsonString = json.toString(0)
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
        Setting.instance.homeTabConfigJsonString = PageConfigUtil.DEFAULT_CONFIG.toString(0)
    }
}
