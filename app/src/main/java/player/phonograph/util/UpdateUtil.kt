/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util

import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import player.phonograph.BuildConfig
import player.phonograph.UpdateConfig.requestUriBitBucket
import player.phonograph.UpdateConfig.requestUriFastGit
import player.phonograph.UpdateConfig.requestUriGitHub
import player.phonograph.UpdateConfig.requestUriJsdelivr
import lib.phonograph.misc.VersionJson
import lib.phonograph.misc.VersionJson.Companion.DOWNLOAD_SOURCES
import lib.phonograph.misc.VersionJson.Companion.DOWNLOAD_URIS
import lib.phonograph.misc.VersionJson.Companion.EN
import lib.phonograph.misc.VersionJson.Companion.LOG_SUMMARY
import lib.phonograph.misc.VersionJson.Companion.UPGRADABLE
import lib.phonograph.misc.VersionJson.Companion.VERSION
import lib.phonograph.misc.VersionJson.Companion.VERSIONCODE
import lib.phonograph.misc.VersionJson.Companion.ZH_CN
import lib.phonograph.misc.VersionJson.Companion.separator
import player.phonograph.misc.webRequest
import player.phonograph.settings.Setting
import player.phonograph.util.Util.debug
import java.io.IOException

object UpdateUtil {

    private const val TAG = "UpdateUtil"

    /**
     * @return version bundle or null (failure)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun checkUpdate(force: Boolean = false): Bundle? {
        if (!force && !Setting.instance.checkUpgradeAtStartup) {
            Log.d(TAG, "ignore upgrade check!")
            return null
        }
        return withContext(Dispatchers.IO + SupervisorJob()) {
            // source
            val requestGithub = Request.Builder()
                .url(requestUriGitHub).get().build()

            // mirrors
            val requestBitBucket = Request.Builder()
                .url(requestUriBitBucket).get().build()
            val requestJsdelivr = Request.Builder()
                .url(requestUriJsdelivr).get().build()
            val requestFastGit = Request.Builder()
                .url(requestUriFastGit).get().build()

            // check source first
            checkUpdate(requestGithub)?.let { response ->
                logSucceed(response.request.url)
                val result = process(response, force)
                result?.let { return@withContext it }
            }
            // check the fastest mirror
            val result = select<Response?> {
                produce {
                    send(checkUpdate(requestBitBucket))
                }
                produce {
                    send(checkUpdate(requestJsdelivr))
                }
                produce {
                    send(checkUpdate(requestFastGit))
                }
            }
            return@withContext if (result != null) {
                process(result, force)
            } else {
                null
            }
        }
    }

    private suspend fun checkUpdate(source: Request): Response? {
        return try {
            webRequest(request = source)
        } catch (e: IOException) {
            logFails(source.url)
            null
        }
    }

    /**
     * handle response
     * @return resolved result
     */
    private suspend fun process(response: Response, force: Boolean): Bundle? {
        return withContext(Dispatchers.Default) {

            val versionJson: VersionJson? =
                try {
                    response.body?.use {
                        VersionJson.parseFromJson(JSONObject(it.string()))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Parse version.json fail!")
                    e.printStackTrace()
                    return@withContext null
                }


            return@withContext versionJson?.let { json: VersionJson ->
//                debug {
//                    Log.v(
//                        TAG,
//                        "versionCode: ${json.versionCode}, version: ${json.version}, logSummary-zh: ${json.logSummaryZH}, logSummary-en: ${json.logSummaryEN}"
//                    )
//                }

                val ignoreUpgradeVersionCode = Setting.instance.ignoreUpgradeVersionCode
                debug {
                    Log.v(
                        TAG,
                        "current state: force:$force, ignoreUpgradeVersionCode:$ignoreUpgradeVersionCode, canAccessGitHub:$canAccessGitHub "
                    )
                }

                // stop if version code is lower ignore version code level and not force to execute
                if (ignoreUpgradeVersionCode >= json.versionCode && !force) {
                    Log.d(TAG, "ignore this upgrade(version code: ${json.versionCode})")
                    return@let null
                }

                val result = Bundle().also {
                    it.putInt(VERSIONCODE, json.versionCode)
                    it.putString(VERSION, json.version)
                    // it.putString(LOG_SUMMARY, json.logSummary)
                    it.putString("${ZH_CN}${separator}${LOG_SUMMARY}", json.logSummaryZH)
                    it.putString("${EN}${separator}${LOG_SUMMARY}", json.logSummaryEN)
                    it.putBoolean(CAN_ACCESS_GITHUB, canAccessGitHub)
                    if (json.downloadUris != null && json.downloadSources != null) {
                        it.putStringArray(DOWNLOAD_URIS, json.downloadUris)
                        it.putStringArray(DOWNLOAD_SOURCES, json.downloadSources)
                    }
                    if (force) {
                        it.putBoolean(UPGRADABLE, true)
                    } else {
                        it.putBoolean(UPGRADABLE, false)
                    }
                }
                if (json.versionCode > BuildConfig.VERSION_CODE) {
                    debug { Log.v(TAG, "updatable!") }
                    result.putBoolean(UPGRADABLE, true)
                } else if (json.versionCode == BuildConfig.VERSION_CODE) {
                    debug { Log.v(TAG, "no update, latest version!") }
                } else if (json.versionCode < BuildConfig.VERSION_CODE) {
                    debug { Log.w(TAG, "no update, version is newer than latest?") }
                }
                return@let result
            }
        }
    }

    internal var canAccessGitHub = false

    private fun logFails(url: HttpUrl) =
        Log.w(TAG, "Failed to check new version from $url!")

    private fun logSucceed(url: HttpUrl) = debug {
        Log.i(TAG, "Succeeded to check new version from $url!")
    }


    const val CAN_ACCESS_GITHUB = "canAccessGitHub"
}
