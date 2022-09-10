/*
 * Copyright (c) 2022 chr_56
 */
@file:JvmName("UpdateJsonParser")

package player.phonograph.misc

import androidx.annotation.Keep
import org.json.JSONObject

@Keep
class VersionJson {
    @JvmField
    var version: String? = ""

    @JvmField
    var versionCode: Int = 0

//        @JvmField
//        var logSummary: String? = ""

    var logSummaryZH: String = ""

    var logSummaryEN: String = ""

    @JvmField
    var downloadSources: Array<String>? = null

    @JvmField
    var downloadUris: Array<String>? = null

    companion object {
        fun parseFromJson(json: JSONObject): VersionJson {
            val version = VersionJson()

            version.version = json.optString(VERSION)
            version.versionCode = json.optInt(VERSIONCODE)
            // version.logSummary = json.optString(LOG_SUMMARY)
            version.logSummaryZH = json.optJSONObject(ZH_CN)?.optString(LOG_SUMMARY) ?: "NOT FOUND"
            version.logSummaryEN = json.optJSONObject(EN)?.optString(LOG_SUMMARY) ?: "NOT FOUND"

            val downloadSourcesArray = json.optJSONArray(DOWNLOAD_SOURCES)
            version.downloadSources =
                downloadSourcesArray?.let { array ->
                    Array<String>(array.length()) { i -> array.optString(i) }
                }

            val downloadUrisArray = json.optJSONArray(DOWNLOAD_URIS)
            version.downloadUris =
                downloadUrisArray?.let { array ->
                    Array<String>(array.length()) { i -> array.optString(i) }
                }

            return version
        }

        const val VERSION = "version"
        const val VERSIONCODE = "versionCode"
        const val LOG_SUMMARY = "logSummary"
        const val DOWNLOAD_URIS = "downloadUris"
        const val DOWNLOAD_SOURCES = "downloadSources"
        const val UPGRADABLE = "upgradable"

        const val ZH_CN = "zh-cn"
        const val EN = "en"
        const val separator = ":"

    }
}


