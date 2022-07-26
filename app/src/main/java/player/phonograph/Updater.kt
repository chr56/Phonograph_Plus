/*
 * Copyright (c) 2021 chr_56
 */

package player.phonograph

import android.os.Bundle
import android.util.Log
import androidx.annotation.Keep
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.settings.Setting
import java.io.IOException
import java.util.concurrent.TimeUnit

// TODO: better ways to ignore jsdelivr

object Updater {
    /**
     * @param callback a callback that would be executed if there's newer version ()
     * @param force true if you want to execute callback no mater there is no newer version or automatic check is disabled
     */
    fun checkUpdate(callback: (Bundle) -> Unit, force: Boolean = false) {
        if (!force && !Setting.instance.checkUpgradeAtStartup) {
            Log.d(TAG, "ignore upgrade check!"); return
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .build()

        blockLock = false // unlock
        blockLockHolder = ""

        val requestGithub = Request.Builder()
            .url(requestUriGitHub).get().build()
        val requestBitBucket = Request.Builder()
            .url(requestUriBitBucket).get().build()
        val requestJsdelivr = Request.Builder()
            .url(requestUriJsdelivr).get().build()
        val requestFastGit = Request.Builder()
            .url(requestUriFastGit).get().build()

        sendRequest(
            okHttpClient,
            requestGithub,
            { call: Call, _: IOException ->
                logFails(call)
                canAccessGitHub = false
            },
            { call: Call, response: Response ->
                canAccessGitHub = true
                handleResponse(callback, force, call, response) // Github is on the highest priority
            }
        )
        sendRequest(
            okHttpClient,
            requestBitBucket,
            { call: Call, _: IOException ->
                logFails(call)
            },
            { call: Call, response: Response ->
                if (!blockLock || blockLockHolder.contains("jsdelivr.net")) handleResponse(
                    callback,
                    force,
                    call,
                    response
                ) else logIgnored(call)
            }
        )
        sendRequest(
            okHttpClient,
            requestJsdelivr,
            { call: Call, _: IOException ->
                logFails(call)
            },
            { call: Call, response: Response ->
                if (!blockLock) handleResponse(callback, force, call, response) else logIgnored(
                    call
                )
            }
        )
        sendRequest(
            okHttpClient,
            requestFastGit,
            { call: Call, _: IOException ->
                logFails(call)
            },
            { call: Call, response: Response ->
                if (!blockLock || blockLockHolder.contains("jsdelivr.net")) handleResponse(
                    callback,
                    force,
                    call,
                    response
                ) else logIgnored(call)
            }
        )
    }

    private inline fun sendRequest(
        client: OkHttpClient,
        request: Request,
        crossinline failureCallback: (Call, IOException) -> Unit,
        crossinline successCallback: (Call, Response) -> Unit
    ): Call {
        return client.newCall(request).apply {
            this.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) =
                    failureCallback(call, e)

                override fun onResponse(call: Call, response: Response) =
                    successCallback(call, response)
            })
        }
    }

    private fun logFails(call: Call) =
        Log.w(TAG, "Fail to check new version! callUri = ${call.request().url}")

    private fun logIgnored(call: Call) =
        Log.i(TAG, "Succeed to check new version, but it was blocked by an early successful call! callUri = ${call.request().url}")

    private fun logSucceed(call: Call) =
        Log.i(TAG, "Succeed to check new version! callUri = ${call.request().url}")

    var blockLock: Boolean = false
    var blockLockHolder: String = ""

    private fun handleResponse(
        callback: (Bundle) -> Unit,
        force: Boolean,
        call: Call,
        response: Response
    ) {
        blockLock = true // block other successful call
        blockLockHolder = call.request().url.host
        if (DEBUG) Log.d(TAG, "blockLockHolder:$blockLockHolder")

        logSucceed(call)

        val responseBody = response.body ?: return

        var versionJson: VersionJson? = null
        try {
            versionJson = parseJson(JSONObject(responseBody.string()))
        } catch (e: Exception) {
            blockLock = false
            blockLockHolder = ""
            Log.e(TAG, "Parse version.json fail!")
            e.printStackTrace()
        }

        versionJson?.let { json: VersionJson ->
            if (DEBUG) Log.v(
                TAG,
                "versionCode: ${json.versionCode}, version: ${json.version}, logSummary-zh: ${json.logSummaryZH}, logSummary-en: ${json.logSummaryEN}"
            )

            val ignoreUpgradeVersionCode = Setting.instance.ignoreUpgradeVersionCode
            if (DEBUG) Log.v(
                TAG,
                "current state: force:$force, ignoreUpgradeVersionCode:$ignoreUpgradeVersionCode, canAccessGitHub:$canAccessGitHub "
            )

            // stop if version code is lower ignore version code level and not force to execute
            if (
                ignoreUpgradeVersionCode >= json.versionCode &&
                !force
            ) {
                Log.d(TAG, "ignore this upgrade(version code: ${json.versionCode})")
                return@handleResponse
            }

            val result = Bundle().also {
                it.putInt(VERSIONCODE, json.versionCode)
                it.putString(VERSION, json.version)
//                it.putString(LOG_SUMMARY, json.logSummary)
                it.putString("$ZH_CN$separator$LOG_SUMMARY", json.logSummaryZH)
                it.putString("$EN$separator$LOG_SUMMARY", json.logSummaryEN)
                it.putBoolean(CAN_ACCESS_GITHUB, canAccessGitHub)
                if (json.downloadUris != null && json.downloadSources != null) {
                    it.putStringArray(DOWNLOAD_URIS, json.downloadUris)
                    it.putStringArray(DOWNLOAD_SOURCES, json.downloadSources)
                }
            }

            when {
                json.versionCode > BuildConfig.VERSION_CODE -> {
                    Log.v(TAG, "updatable!")
                    result.putBoolean(UPGRADABLE, true)
                    callback.invoke(result)
                }
                json.versionCode == BuildConfig.VERSION_CODE -> {
                    Log.v(TAG, "no update, latest version!")
                    if (force) {
                        result.putBoolean(UPGRADABLE, false)
                        callback.invoke(result)
                    }
                }
                json.versionCode < BuildConfig.VERSION_CODE -> {
                    Log.w(TAG, "no update, version is newer than latest?")
                    if (force) {
                        result.putBoolean(UPGRADABLE, false)
                        callback.invoke(result)
                    }
                }
            }
        }
    }
    private var canAccessGitHub: Boolean = false

    @Keep
    class VersionJson {
//        @JvmField
        var version: String? = ""

//        @JvmField
        var versionCode: Int = 0

//        @JvmField
//        var logSummary: String? = ""

        var logSummaryZH: String = ""

        var logSummaryEN: String = ""

//        @JvmField
        var downloadSources: Array<String>? = null

//        @JvmField
        var downloadUris: Array<String>? = null
    }

    private fun parseJson(json: JSONObject): VersionJson {
        val version = VersionJson()

        version.version = json.optString(VERSION)
        version.versionCode = json.optInt(VERSIONCODE)
//        version.logSummary = json.optString(LOG_SUMMARY)
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

    private const val owner = "chr56"
    private const val organization = "Phonograph-Plus"
    private const val repo = "Phonograph_Plus"
    private const val branch = "dev"
    private const val file = "version.json"

    private const val requestUriGitHub = "https://raw.githubusercontent.com/$owner/$repo/$branch/$file"
    private const val requestUriBitBucket = "https://bitbucket.org/$organization/$repo/raw/$branch/$file"

    private const val requestUriJsdelivr = "https://cdn.jsdelivr.net/gh/$owner/$repo@$branch/$file"
    private const val requestUriFastGit = "https://endpoint.fastgit.org/https://github.com/$owner/$repo/blob/$branch/$file"

    private const val TAG = "Updater"

    const val VERSION = "version"
    const val VERSIONCODE = "versionCode"
    const val LOG_SUMMARY = "logSummary"
    const val DOWNLOAD_URIS = "downloadUris"
    const val DOWNLOAD_SOURCES = "downloadSources"
    const val UPGRADABLE = "upgradable"

    const val ZH_CN = "zh-cn"
    const val EN = "en"
    const val separator = ":"

    const val CAN_ACCESS_GITHUB = "canAccessGitHub"
}
