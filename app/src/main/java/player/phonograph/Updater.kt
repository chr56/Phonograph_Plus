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
import player.phonograph.util.PreferenceUtil
import java.io.IOException
import java.util.concurrent.TimeUnit

// TODO: better ways to ignore jsdelivr

object Updater {
    /**
     * @param callback a callback that would be executed if there's newer version ()
     * @param force true if you want to execute callback no mater there is no newer version or automatic check is disabled
     */
    fun checkUpdate(callback: (Bundle) -> Unit, force: Boolean = false) {
        if (!force && !PreferenceUtil.getInstance(App.instance).checkUpgradeAtStartup) {
            Log.w(TAG, "ignore upgrade check!"); return
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
            okHttpClient, requestGithub,
            { call: Call, _: IOException ->
                logFails(call)
                canAccessGitHub = false
            },
            { call: Call, response: Response ->
                handleResponse(callback, force, call, response) // Github is on the highest priority
                canAccessGitHub = true
            }
        )
        sendRequest(
            okHttpClient, requestBitBucket,
            { call: Call, _: IOException ->
                logFails(call)
            },
            { call: Call, response: Response ->
                if (!blockLock || blockLockHolder.contains("jsdelivr.net")) handleResponse(callback, force, call, response) else logIgnored(call)
            }
        )
        sendRequest(
            okHttpClient, requestJsdelivr,
            { call: Call, _: IOException ->
                logFails(call)
            },
            { call: Call, response: Response ->
                if (!blockLock) handleResponse(callback, force, call, response) else logIgnored(call)
            }
        )
        sendRequest(
            okHttpClient, requestFastGit,
            { call: Call, _: IOException ->
                logFails(call)
            },
            { call: Call, response: Response ->
                if (!blockLock || blockLockHolder.contains("jsdelivr.net")) handleResponse(callback, force, call, response) else logIgnored(call)
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
        Log.e(TAG, "Fail to check new version! callUri = ${call.request().url()}")

    private fun logIgnored(call: Call) =
        Log.i(TAG, "Succeed to check new version, but it was blocked by an early successful call! callUri = ${call.request().url()}")

    private fun logSucceed(call: Call) =
        Log.i(TAG, "Succeed to check new version! callUri = ${call.request().url()}")

    var blockLock: Boolean = false
    var blockLockHolder: String = ""

    private fun handleResponse(callback: (Bundle) -> Unit, force: Boolean, call: Call, response: Response) {
        blockLock = true // block other successful call
        blockLockHolder = call.request().url().host() ; Log.d(TAG, "blockLockHolder:$blockLockHolder")

        logSucceed(call)

        val responseBody = response.body() ?: return

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
            Log.i(TAG, "versionCode: ${json.versionCode}, version: ${json.version}, logSummary: ${json.logSummary}")

            val ignoreUpgradeVersionCode = PreferenceUtil(App.instance).ignoreUpgradeVersionCode
            Log.d(TAG, "current state: force:$force, ignoreUpgradeVersionCode:$ignoreUpgradeVersionCode, CanAccessGitHub:$canAccessGitHub ")

            // stop if version code is lower ignore version code level and not force to execute
            if (
                ignoreUpgradeVersionCode >= json.versionCode &&
                !force
            ) {
                Log.d(TAG, "ignore this upgrade(version code: ${json.versionCode})")
                return@handleResponse
            }

            val result = Bundle().also {
                it.putInt(VersionCode, json.versionCode)
                it.putString(Version, json.version)
                it.putString(LogSummary, json.logSummary)
                it.putBoolean(CanAccessGitHub, canAccessGitHub)
                if (json.downloadUris != null && json.downloadSources != null) {
                    it.putStringArray(DownloadUris, json.downloadUris)
                    it.putStringArray(DownloadSources, json.downloadSources)
                }
            }

            when {
                json.versionCode > BuildConfig.VERSION_CODE -> {
                    Log.i(TAG, "updatable!")
                    result.putBoolean(Upgradable, true)
                    callback.invoke(result)
                }
                json.versionCode == BuildConfig.VERSION_CODE -> {
                    Log.i(TAG, "no update, latest version!")
                    if (force) {
                        result.putBoolean(Upgradable, false)
                        callback.invoke(result)
                    }
                }
                json.versionCode < BuildConfig.VERSION_CODE -> {
                    Log.w(TAG, "no update, version is newer than latest?")
                    if (force) {
                        result.putBoolean(Upgradable, false)
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
        var logSummary: String? = ""

//        @JvmField
        var downloadSources: Array<String>? = null

//        @JvmField
        var downloadUris: Array<String>? = null
    }

    private fun parseJson(json: JSONObject): VersionJson {
        val version = VersionJson()

        version.version = json.optString(Version)
        version.versionCode = json.optInt(VersionCode)
        version.logSummary = json.optString(LogSummary)

        val downloadSourcesArray = json.optJSONArray(DownloadSources)
        version.downloadSources =
            downloadSourcesArray?.let { array ->
                Array<String>(array.length()) { i -> array.optString(i) }
            }

        val downloadUrisArray = json.optJSONArray(DownloadUris)
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

    const val Version = "version"
    const val VersionCode = "versionCode"
    const val LogSummary = "logSummary"
    const val DownloadUris = "downloadUris"
    const val DownloadSources = "downloadSources"
    const val Upgradable = "upgradable"

    const val CanAccessGitHub = "canAccessGitHub"
}
